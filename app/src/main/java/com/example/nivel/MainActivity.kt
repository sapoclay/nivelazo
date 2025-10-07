package com.example.nivel

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nivel.ui.theme.NivelTheme
import kotlinx.coroutines.delay
import kotlin.math.abs
import java.util.Locale

enum class NivelMode {
    HORIZONTAL,  // Teléfono plano
    VERTICAL     // Teléfono de pie
}

enum class Screen {
    SPLASH,
    NIVEL,
    ABOUT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NivelTheme {
                NivelApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NivelApp() {
    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }

    // Efecto para cambiar de splash a pantalla principal después de 4 segundos
    LaunchedEffect(Unit) {
        delay(4000) // 4 segundos
        currentScreen = Screen.NIVEL
    }

    when (currentScreen) {
        Screen.SPLASH -> SplashScreen()
        Screen.NIVEL, Screen.ABOUT -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (currentScreen == Screen.NIVEL) "NivelAzo" else "Acerca de",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        ),
                        navigationIcon = {
                            if (currentScreen == Screen.ABOUT) {
                                IconButton(onClick = { currentScreen = Screen.NIVEL }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                                }
                            }
                        },
                        actions = {
                            if (currentScreen == Screen.NIVEL) {
                                IconButton(onClick = { currentScreen = Screen.ABOUT }) {
                                    Icon(Icons.Default.Info, contentDescription = "Acerca de")
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                when (currentScreen) {
                    Screen.NIVEL -> NivelScreen(modifier = Modifier.padding(innerPadding))
                    Screen.ABOUT -> AboutScreen(modifier = Modifier.padding(innerPadding))
                    Screen.SPLASH -> {} // No debería llegar aquí
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo NivelAzo",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "NivelAzo",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 42.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cargando...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun NivelScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }
    var rawX by remember { mutableFloatStateOf(0f) }
    var rawY by remember { mutableFloatStateOf(0f) }
    var rawZ by remember { mutableFloatStateOf(0f) }
    var mode by remember { mutableStateOf(NivelMode.HORIZONTAL) }

    // Detectar si la pantalla está en modo landscape
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Log.e("NivelApp", "No hay acelerómetro disponible")
        } else {
            Log.d("NivelApp", "Acelerómetro encontrado: ${accelerometer.name}")
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                var x = event.values[0]  // Eje X (izquierda-derecha)
                var y = event.values[1]  // Eje Y (arriba-abajo)
                val z = event.values[2]  // Eje Z (perpendicular a la pantalla)

                rawX = x
                rawY = y
                rawZ = z

                // Obtener la rotación actual de la pantalla
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val rotation = windowManager.defaultDisplay.rotation

                // Detectar automáticamente la orientación del teléfono
                val absX = abs(x)
                val absY = abs(y)
                val absZ = abs(z)

                // Determinar el modo según qué eje tiene mayor valor ABSOLUTO
                // Z > 6.0 significa que la gravedad apunta perpendicular a la pantalla = teléfono plano
                // Si Y o X son dominantes, el teléfono está de pie
                mode = if (absZ > 6.0f) {
                    // Z dominante = teléfono plano (horizontal)
                    NivelMode.HORIZONTAL
                } else {
                    // Y o X dominantes = teléfono vertical
                    NivelMode.VERTICAL
                }

                when (mode) {
                    NivelMode.HORIZONTAL -> {
                        // Modo horizontal: teléfono plano sobre superficie
                        // Compensar la rotación de la pantalla para que los cálculos sean correctos
                        val adjustedX: Float
                        val adjustedY: Float

                        when (rotation) {
                            Surface.ROTATION_0 -> {
                                // Portrait normal - sin cambios
                                adjustedX = x
                                adjustedY = y
                            }
                            Surface.ROTATION_90 -> {
                                // Landscape (rotado 90° a la derecha)
                                // La pantalla está horizontal, pero el teléfono sigue plano
                                adjustedX = -y
                                adjustedY = x
                            }
                            Surface.ROTATION_180 -> {
                                // Portrait invertido
                                adjustedX = -x
                                adjustedY = -y
                            }
                            Surface.ROTATION_270 -> {
                                // Landscape (rotado 90° a la izquierda)
                                adjustedX = y
                                adjustedY = -x
                            }
                            else -> {
                                adjustedX = x
                                adjustedY = y
                            }
                        }

                        roll = Math.toDegrees(kotlin.math.atan2(adjustedX.toDouble(), z.toDouble())).toFloat()
                        pitch = Math.toDegrees(kotlin.math.atan2(adjustedY.toDouble(), z.toDouble())).toFloat()
                    }
                    NivelMode.VERTICAL -> {
                        // Modo vertical: teléfono de pie apoyado en lateral
                        // Aquí también necesitamos compensar la rotación de la pantalla
                        val adjustedX: Float
                        val adjustedY: Float
                        val adjustedZ: Float

                        when (rotation) {
                            Surface.ROTATION_0 -> {
                                // Portrait normal
                                adjustedX = x
                                adjustedY = y
                                adjustedZ = z
                            }
                            Surface.ROTATION_90 -> {
                                // Landscape - el teléfono está de pie, pantalla rotada
                                adjustedX = -y
                                adjustedY = x
                                adjustedZ = z
                            }
                            Surface.ROTATION_180 -> {
                                // Portrait invertido
                                adjustedX = -x
                                adjustedY = -y
                                adjustedZ = z
                            }
                            Surface.ROTATION_270 -> {
                                // Landscape invertido
                                adjustedX = y
                                adjustedY = -x
                                adjustedZ = z
                            }
                            else -> {
                                adjustedX = x
                                adjustedY = y
                                adjustedZ = z
                            }
                        }

                        // Calcular ángulos basados en la gravedad con valores ajustados
                        val gravityNorm = kotlin.math.sqrt((adjustedX * adjustedX + adjustedY * adjustedY + adjustedZ * adjustedZ).toDouble()).toFloat()

                        // Roll: inclinación lateral
                        roll = Math.toDegrees(kotlin.math.asin((adjustedX / gravityNorm).toDouble().coerceIn(-1.0, 1.0))).toFloat()

                        // Pitch: inclinación frontal
                        pitch = Math.toDegrees(kotlin.math.asin((adjustedZ / gravityNorm).toDouble().coerceIn(-1.0, 1.0))).toFloat()
                    }
                }

                Log.d("NivelApp", "Mode: $mode, Rotation: $rotation, Landscape: $isLandscape, X: $x, Y: $y, Z: $z, Roll: $roll, Pitch: $pitch")
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                Log.d("NivelApp", "Precisión del sensor cambiada: $accuracy")
            }
        }

        val registered = sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        Log.d("NivelApp", "Sensor registrado: $registered")

        onDispose {
            sensorManager.unregisterListener(listener)
            Log.d("NivelApp", "Sensor desregistrado")
        }
    }

    // Ajustar tamaños según el modo y la orientación de pantalla
    val bubbleSize = when {
        isLandscape -> 180.dp  // Más pequeño en landscape
        mode == NivelMode.HORIZONTAL -> 300.dp
        else -> 200.dp
    }
    val titleFontSize = when {
        isLandscape -> 18.sp
        mode == NivelMode.HORIZONTAL -> 28.sp
        else -> 20.sp
    }
    val spacerHeight = if (isLandscape) 8.dp else if (mode == NivelMode.HORIZONTAL) 24.dp else 12.dp
    val showSensorValues = mode == NivelMode.HORIZONTAL && !isLandscape

    if (isLandscape) {
        // Layout horizontal (landscape) - Organizar en fila
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lado izquierdo: Burbuja
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                NivelBurbuja(
                    pitch = pitch,
                    roll = roll,
                    modifier = Modifier.size(bubbleSize)
                )
            }

            // Lado derecho: Información
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Nivel de Burbuja",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = titleFontSize
                )

                Spacer(modifier = Modifier.height(spacerHeight))

                Text(
                    text = if (mode == NivelMode.HORIZONTAL) "Modo: Horizontal (plano)" else "Modo: Vertical (de pie)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )

                Text(
                    text = "Detección automática",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Inclinación horizontal: ${String.format(Locale.US, "%.1f°", roll)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 14.sp
                )
                Text(
                    text = "Inclinación vertical: ${String.format(Locale.US, "%.1f°", pitch)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                val isLevel = abs(pitch) < 2f && abs(roll) < 2f
                Text(
                    text = if (isLevel) "¡NIVELADO!" else "Ajustar posición",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isLevel) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    } else {
        // Layout vertical (portrait) - Organizar en columna
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Nivel de Burbuja",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = titleFontSize
            )

            Spacer(modifier = Modifier.height(spacerHeight))

            NivelBurbuja(
                pitch = pitch,
                roll = roll,
                modifier = Modifier.size(bubbleSize)
            )

            Spacer(modifier = Modifier.height(spacerHeight))

            // Indicador de modo automático
            Text(
                text = if (mode == NivelMode.HORIZONTAL) "Modo: Horizontal (plano)" else "Modo: Vertical (de pie)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = if (mode == NivelMode.HORIZONTAL) 16.sp else 14.sp
            )

            Text(
                text = "Detección automática",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = if (mode == NivelMode.HORIZONTAL) 12.sp else 10.sp
            )

            Spacer(modifier = Modifier.height(if (mode == NivelMode.HORIZONTAL) 12.dp else 8.dp))

            // Valores del sensor (solo en modo horizontal portrait para no saturar la pantalla)
            if (showSensorValues) {
                Text(
                    text = "Valores del sensor:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "X: ${String.format(Locale.US, "%.2f", rawX)} | Y: ${String.format(Locale.US, "%.2f", rawY)} | Z: ${String.format(Locale.US, "%.2f", rawZ)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "Inclinación horizontal: ${String.format(Locale.US, "%.1f°", roll)}",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = if (mode == NivelMode.HORIZONTAL) 16.sp else 14.sp
            )
            Text(
                text = "Inclinación vertical: ${String.format(Locale.US, "%.1f°", pitch)}",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = if (mode == NivelMode.HORIZONTAL) 16.sp else 14.sp
            )

            Spacer(modifier = Modifier.height(if (mode == NivelMode.HORIZONTAL) 16.dp else 12.dp))

            val isLevel = abs(pitch) < 2f && abs(roll) < 2f
            Text(
                text = if (isLevel) "¡NIVELADO!" else "Ajustar posición",
                style = MaterialTheme.typography.titleLarge,
                color = if (isLevel) Color(0xFF4CAF50) else Color(0xFFF44336),
                fontWeight = FontWeight.Bold,
                fontSize = if (mode == NivelMode.HORIZONTAL) 24.sp else 20.sp
            )
        }
    }
}

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo NivelAzo",
            modifier = Modifier.size(150.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Título
        Text(
            text = "NivelAzo",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 36.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Versión 1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Descripción
        Text(
            text = "NivelAzo es una aplicación de nivel digital que te permite medir la inclinación de superficies en tiempo real usando los sensores de tu dispositivo.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Características:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "• Modo horizontal para superficies planas\n" +
                   "• Modo vertical para paredes y superficies verticales\n" +
                   "• Medición precisa en grados\n" +
                   "• Indicador visual con burbuja\n" +
                   "• Interfaz intuitiva y fácil de usar",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Botón para abrir GitHub
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sapoclay/nivelazo"))
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(56.dp)
        ) {
            Text(
                text = "Ver en GitHub",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pie de página
        Text(
            text = "© 2025 NivelAzo\nDesarrollado por entreunosyceros",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun NivelBurbuja(pitch: Float, roll: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxOffset = size.width / 2 - 60f

        // Fondo del nivel
        drawCircle(
            color = Color(0xFFEEEEEE),
            radius = maxOffset + 30f,
            center = Offset(centerX, centerY)
        )

        // Líneas de referencia
        drawLine(
            color = Color(0xFF9E9E9E),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFF9E9E9E),
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = 3f
        )

        // Círculo contenedor
        drawCircle(
            color = Color.Black,
            radius = maxOffset + 30f,
            center = Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )

        // Círculo interno de referencia
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = 15f,
            center = Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Calcular posición de la burbuja
        // Normalizar el ángulo a un rango de -1 a 1
        val normalizedRoll = (roll / 30f).coerceIn(-1f, 1f)
        val normalizedPitch = (pitch / 30f).coerceIn(-1f, 1f)

        val bubbleX = centerX + normalizedRoll * maxOffset
        val bubbleY = centerY + normalizedPitch * maxOffset

        // Burbuja (verde cuando está nivelado)
        val isLevel = abs(pitch) < 2f && abs(roll) < 2f

        // Sombra de la burbuja
        drawCircle(
            color = Color(0x44000000),
            radius = 42f,
            center = Offset(bubbleX + 2f, bubbleY + 2f)
        )

        // Burbuja principal
        drawCircle(
            color = if (isLevel) Color(0xFF4CAF50) else Color(0xFFFFEB3B),
            radius = 40f,
            center = Offset(bubbleX, bubbleY)
        )

        // Contorno de la burbuja
        drawCircle(
            color = Color.Black,
            radius = 40f,
            center = Offset(bubbleX, bubbleY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )

        // Reflejo en la burbuja
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = 15f,
            center = Offset(bubbleX - 10f, bubbleY - 10f)
        )
    }
}