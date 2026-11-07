package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.R
import com.example.viewmodel.AuthViewModel

enum class AuthTab {
    LOGIN,
    REGISTER
}

@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel) {
    AuthScreenUnified(
        initialTab = AuthTab.LOGIN,
        navController = navController,
        authViewModel = authViewModel
    )
}

@Composable
fun RegisterScreen(navController: NavController, authViewModel: AuthViewModel) {
    AuthScreenUnified(
        initialTab = AuthTab.REGISTER,
        navController = navController,
        authViewModel = authViewModel
    )
}

@Composable
fun AuthScreenUnified(
    initialTab: AuthTab,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(initialTab) }

    // Form states for Login
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Form states for Daftar (Register)
    var registerName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var registerPasswordVisible by remember { mutableStateOf(false) }

    val isLoading by authViewModel.isLoading.collectAsState()

    LaunchedEffect(key1 = true) {
        authViewModel.errorEvent.collect { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(key1 = true) {
        authViewModel.authSuccessEvent.collect {
            val message = if (selectedTab == AuthTab.LOGIN) "Selamat datang kembali!" else "Pendaftaran berhasil! Akun Anda telah disimpan."
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            navController.navigate("main") {
                popUpTo("login") { inclusive = true }
                popUpTo("register") { inclusive = true }
            }
        }
    }

    // Outer Background with Top Sky Blue Gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2979FF), // Vibrant Sky/Royal Blue
                        Color(0xFF3B82F6),
                        Color(0xFF60A5FA),
                        Color(0xFFBFDBFE),
                        Color(0xFFF0F6FF)
                    )
                )
            )
    ) {
        // Decorative background top curves
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            // Subtle abstract circular glow
            Surface(
                modifier = Modifier
                    .size(240.dp)
                    .offset(x = (-40).dp, y = (-50).dp),
                shape = RoundedCornerShape(120.dp),
                color = Color.White.copy(alpha = 0.08f)
            ) {}
            Surface(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (-20).dp),
                shape = RoundedCornerShape(140.dp),
                color = Color.White.copy(alpha = 0.06f)
            ) {}
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // App Logo Squircle with MoneyFlow Wallet Icon
            Image(
                painter = painterResource(id = R.drawable.ic_moneyflow_logo),
                contentDescription = "MoneyFlow Logo",
                modifier = Modifier
                    .size(76.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color(0x33000000),
                        spotColor = Color(0x33000000)
                    )
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // App Title
            Text(
                text = "MoneyFlow",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "Kelola keuanganmu dengan mudah",
                fontSize = 13.5.sp,
                color = Color(0xFF334155),
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main White Card Container with Rounded Top Corners
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        ambientColor = Color(0x22000000),
                        spotColor = Color(0x22000000)
                    ),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    // Bottom Decorative Chart Bars & Waves matching reference screenshot
                    BottomDecorativeFinancialChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .align(Alignment.BottomCenter)
                    )

                    // Scrollable Form Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Segmented Pill Tab Switcher (Login vs Daftar)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Login Tab
                                val isLoginSelected = selectedTab == AuthTab.LOGIN
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isLoginSelected) Color(0xFF2563EB) else Color.Transparent
                                        )
                                        .clickable { selectedTab = AuthTab.LOGIN }
                                        .testTag("tab_login"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Login,
                                            contentDescription = "Login",
                                            tint = if (isLoginSelected) Color.White else Color(0xFF475569),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Login",
                                            fontSize = 15.sp,
                                            fontWeight = if (isLoginSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isLoginSelected) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }

                                // Daftar Tab
                                val isRegisterSelected = selectedTab == AuthTab.REGISTER
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isRegisterSelected) Color(0xFF2563EB) else Color.Transparent
                                        )
                                        .clickable { selectedTab = AuthTab.REGISTER }
                                        .testTag("tab_register"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = "Daftar",
                                            tint = if (isRegisterSelected) Color.White else Color(0xFF475569),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Daftar",
                                            fontSize = 15.sp,
                                            fontWeight = if (isRegisterSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isRegisterSelected) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Crossfade between Login and Register views
                        Crossfade(targetState = selectedTab, label = "AuthTabTransition") { tab ->
                            if (tab == AuthTab.LOGIN) {
                                // ==================== LOGIN VIEW ====================
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Selamat Datang!",
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Masuk ke akun MoneyFlow Anda",
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(22.dp))

                                    // Email Input
                                    OutlinedTextField(
                                        value = loginEmail,
                                        onValueChange = { loginEmail = it },
                                        placeholder = { Text("Email", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = "Email",
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("email_input"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF2563EB),
                                            unfocusedBorderColor = Color(0xFFE2E8F0),
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedTextColor = Color(0xFF0F172A),
                                            unfocusedTextColor = Color(0xFF0F172A)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Password Input
                                    OutlinedTextField(
                                        value = loginPassword,
                                        onValueChange = { loginPassword = it },
                                        placeholder = { Text("Password", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Password",
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "Toggle password visibility",
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("password_input"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF2563EB),
                                            unfocusedBorderColor = Color(0xFFE2E8F0),
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedTextColor = Color(0xFF0F172A),
                                            unfocusedTextColor = Color(0xFF0F172A)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(22.dp))

                                    // Masuk Button
                                    Button(
                                        onClick = {
                                            if (loginEmail.isBlank() || loginPassword.isBlank()) {
                                                Toast.makeText(context, "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                            } else {
                                                authViewModel.login(loginEmail, loginPassword)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("submit_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        enabled = !isLoading
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White,
                                                strokeWidth = 2.5.dp
                                            )
                                        } else {
                                            Text(
                                                text = "Masuk",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Divider "atau"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = Color(0xFFE2E8F0)
                                        )
                                        Text(
                                            text = "atau",
                                            fontSize = 13.sp,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.padding(horizontal = 14.dp)
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = Color(0xFFE2E8F0)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Switch to Register link
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Belum punya akun? ",
                                            fontSize = 14.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = "Daftar Sekarang",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB),
                                            modifier = Modifier.clickable {
                                                selectedTab = AuthTab.REGISTER
                                            }
                                        )
                                    }
                                }
                            } else {
                                // ==================== DAFTAR (REGISTER) VIEW ====================
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Buat Akun Baru",
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Daftar untuk mulai mengelola keuangan",
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(22.dp))

                                    // Nama Pengguna Input
                                    OutlinedTextField(
                                        value = registerName,
                                        onValueChange = { registerName = it },
                                        placeholder = { Text("Nama Pengguna", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Nama Pengguna",
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("name_input"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF2563EB),
                                            unfocusedBorderColor = Color(0xFFE2E8F0),
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedTextColor = Color(0xFF0F172A),
                                            unfocusedTextColor = Color(0xFF0F172A)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Email Input
                                    OutlinedTextField(
                                        value = registerEmail,
                                        onValueChange = { registerEmail = it },
                                        placeholder = { Text("Email", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = "Email",
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("email_input"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF2563EB),
                                            unfocusedBorderColor = Color(0xFFE2E8F0),
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedTextColor = Color(0xFF0F172A),
                                            unfocusedTextColor = Color(0xFF0F172A)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Password Input
                                    OutlinedTextField(
                                        value = registerPassword,
                                        onValueChange = { registerPassword = it },
                                        placeholder = { Text("Password", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Password",
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { registerPasswordVisible = !registerPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (registerPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "Toggle password visibility",
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        visualTransformation = if (registerPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("password_input"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF2563EB),
                                            unfocusedBorderColor = Color(0xFFE2E8F0),
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedTextColor = Color(0xFF0F172A),
                                            unfocusedTextColor = Color(0xFF0F172A)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(22.dp))

                                    // Daftar Button
                                    Button(
                                        onClick = {
                                            if (registerName.isBlank() || registerEmail.isBlank() || registerPassword.isBlank()) {
                                                Toast.makeText(context, "Semua data (Nama, Email, Password) harus diisi", Toast.LENGTH_SHORT).show()
                                            } else {
                                                authViewModel.register(registerName, registerEmail, registerPassword)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("submit_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        enabled = !isLoading
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White,
                                                strokeWidth = 2.5.dp
                                            )
                                        } else {
                                            Text(
                                                text = "Daftar",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Divider "atau"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = Color(0xFFE2E8F0)
                                        )
                                        Text(
                                            text = "atau",
                                            fontSize = 13.sp,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.padding(horizontal = 14.dp)
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = Color(0xFFE2E8F0)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Switch to Login link
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Sudah punya akun? ",
                                            fontSize = 14.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = "Login di sini",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB),
                                            modifier = Modifier.clickable {
                                                selectedTab = AuthTab.LOGIN
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}

/**
 * Renders decorative financial bar chart silhouettes and wave curves at the bottom
 * matching the visual detail in the user-provided design reference.
 */
@Composable
fun BottomDecorativeFinancialChart(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        // Financial Bar Chart Silhouettes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val barHeights = listOf(28.dp, 44.dp, 32.dp, 58.dp, 48.dp, 72.dp, 40.dp, 64.dp, 84.dp, 52.dp)
            barHeights.forEach { height ->
                Surface(
                    modifier = Modifier
                        .width(18.dp)
                        .height(height),
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                    color = Color(0xFFDBEAFE).copy(alpha = 0.45f)
                ) {}
            }
        }

        // Soft bottom gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color(0xFFEFF6FF).copy(alpha = 0.65f),
                            Color(0xFFDBEAFE).copy(alpha = 0.85f)
                        )
                    )
                )
        )
    }
}
