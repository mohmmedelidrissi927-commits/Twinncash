package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.UserEntity
import com.example.data.model.ServiceCategory
import com.example.data.model.UserRole
import com.example.data.remote.attemptAutoSignIn
import com.example.data.remote.authStateFlow
import com.example.data.remote.signOutWithCredentialManager
import com.example.data.repository.TwinCashFirestoreRepository
import com.example.ui.components.TopUpDialog
import com.example.ui.components.TwinCashTopBar
import com.example.ui.components.WhatsAppFab
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminProfitsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MerchantScreen
import com.example.ui.screens.OffersScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.theme.BgWhite
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ErrorBg
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySoftContainer
import com.example.ui.theme.SuccessBg
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.TwinCashButtonShape
import com.example.ui.theme.TwinCashCardShape
import com.example.ui.theme.TwinCashTheme
import com.example.ui.theme.twinCashPrimaryButtonColors
import com.example.ui.viewmodel.SessionUiState
import com.example.ui.viewmodel.TwinCashViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.WHITE, AndroidColor.WHITE),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.WHITE, AndroidColor.WHITE)
        )
        setContent {
            TwinCashTheme {
                TwinCashApp()
            }
        }
    }
}

private data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun TwinCashApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var autoSignInDone by remember { mutableStateOf(auth.currentUser != null) }
    var pendingRegName by remember { mutableStateOf<String?>(null) }
    var pendingRegWhatsapp by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var lastToastIsError by remember { mutableStateOf(false) }

    var queuedToast by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    LaunchedEffect(queuedToast) {
        val toast = queuedToast ?: return@LaunchedEffect
        lastToastIsError = toast.second
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(toast.first)
        queuedToast = null
    }

    LaunchedEffect(Unit) {
        if (!autoSignInDone) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = { autoSignInDone = true },
                onUnauthenticated = { autoSignInDone = true }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite)
    ) {
        val user = currentUser
        if (user == null) {
            if (!autoSignInDone) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyPrimary)
                }
            } else {
                AuthScreen(
                    isBusy = false,
                    onGoogleAuthSuccess = { _, _, fullName, wa ->
                        pendingRegName = fullName
                        pendingRegWhatsapp = wa
                    },
                    onError = { msg -> queuedToast = msg to true }
                )
            }
        } else {
            key(user.uid) {
                AuthenticatedGate(
                    firebaseUser = user,
                    pendingFullName = pendingRegName,
                    pendingWhatsapp = pendingRegWhatsapp,
                    onClearPendingInputs = {
                        pendingRegName = null
                        pendingRegWhatsapp = null
                    },
                    onNotify = { msg, err -> queuedToast = msg to err },
                    onSignOut = {
                        pendingRegName = null
                        pendingRegWhatsapp = null
                        signOutWithCredentialManager(credentialManager, scope) {
                            queuedToast = ("تم تسجيل الخروج بنجاح" to false)
                        }
                    }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 74.dp)
        ) { data ->
            Snackbar(
                shape = RoundedCornerShape(12.dp),
                containerColor = if (lastToastIsError) ErrorBg else SuccessBg,
                contentColor = if (lastToastIsError) ErrorRed else SuccessGreen
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (lastToastIsError) ErrorRed else SuccessGreen
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedGate(
    firebaseUser: FirebaseUser,
    pendingFullName: String?,
    pendingWhatsapp: String?,
    onClearPendingInputs: () -> Unit,
    onNotify: (String, Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val uid = firebaseUser.uid
    val verifiedEmail = firebaseUser.email.orEmpty()
    val suggestedName = firebaseUser.displayName.orEmpty()
    val emailVerified = firebaseUser.isEmailVerified

    val viewModel: TwinCashViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                TwinCashViewModel(
                    repository = TwinCashFirestoreRepository(db),
                    currentUserId = uid,
                    verifiedEmail = verifiedEmail,
                    displayName = suggestedName,
                    isEmailVerified = emailVerified
                )
            }
        }
    )

    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { event ->
            onNotify(event.message, event.isError)
        }
    }

    when (val state = sessionState) {
        SessionUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyPrimary)
            }
        }

        is SessionUiState.NeedsRegistration -> {
            LaunchedEffect(pendingFullName, pendingWhatsapp) {
                if (!pendingFullName.isNullOrBlank() && !pendingWhatsapp.isNullOrBlank()) {
                    val nameArg = pendingFullName
                    val waArg = pendingWhatsapp
                    onClearPendingInputs()
                    viewModel.completeGoogleRegistration(nameArg, waArg)
                }
            }
            AuthScreen(
                isBusy = isBusy,
                verifiedGoogleEmail = state.verifiedEmail,
                suggestedName = pendingFullName ?: state.suggestedName,
                onCompleteRegistration = viewModel::completeGoogleRegistration,
                onSwitchGoogleAccount = onSignOut,
                onError = { msg -> viewModel.notify(msg, isError = true) }
            )
        }

        is SessionUiState.Blocked -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgWhite)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = TwinCashCardShape,
                    colors = CardDefaults.cardColors(containerColor = BgWhite),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "حالة الحساب",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = state.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondarySlate,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onSignOut,
                            colors = twinCashPrimaryButtonColors(),
                            shape = TwinCashButtonShape,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("العودة لتسجيل الدخول", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        is SessionUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgWhite)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = TwinCashCardShape,
                    colors = CardDefaults.cardColors(containerColor = BgWhite),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تعذر تحميل الحساب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySlate,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onSignOut,
                            colors = twinCashPrimaryButtonColors(),
                            shape = TwinCashButtonShape,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        is SessionUiState.Ready -> {
            AuthenticatedAppShell(
                user = state.user,
                isBusy = isBusy,
                viewModel = viewModel,
                onLogout = onSignOut
            )
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    user: UserEntity,
    isBusy: Boolean,
    viewModel: TwinCashViewModel,
    onLogout: () -> Unit
) {
    val role = UserRole.fromKey(user.role)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val services by viewModel.services.collectAsStateWithLifecycle()
    val offers by viewModel.freeFireOffers.collectAsStateWithLifecycle()
    val contests by viewModel.contests.collectAsStateWithLifecycle()

    val customerOrders by viewModel.customerOrders.collectAsStateWithLifecycle()
    val customerTopUps by viewModel.customerTopUps.collectAsStateWithLifecycle()
    val customerDiscountCodes by viewModel.customerDiscountCodes.collectAsStateWithLifecycle()
    val customerContestEntries by viewModel.customerContestEntries.collectAsStateWithLifecycle()

    val merchantOrders by viewModel.merchantOrders.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminOrders by viewModel.adminOrders.collectAsStateWithLifecycle()
    val adminTopUps by viewModel.adminTopUps.collectAsStateWithLifecycle()
    val adminAuditLogs by viewModel.adminAuditLogs.collectAsStateWithLifecycle()
    val adminDiscountCodes by viewModel.adminDiscountCodes.collectAsStateWithLifecycle()
    val adminContestEntries by viewModel.adminContestEntries.collectAsStateWithLifecycle()
    val selectedProfitPeriod by viewModel.selectedProfitPeriod.collectAsStateWithLifecycle()

    var customerTab by rememberSaveable(user.uid) { mutableIntStateOf(0) }
    var adminTab by rememberSaveable(user.uid) { mutableIntStateOf(0) }
    var serviceFilterCategory by remember { mutableStateOf<ServiceCategory?>(null) }
    var showTopUpDialog by remember { mutableStateOf(false) }

    val unreadOrders = remember(customerOrders) {
        customerOrders.filter { !it.isCustomerNotified && it.status != "pending" }
    }
    val unreadTopUps = remember(customerTopUps) {
        customerTopUps.filter { !it.isCustomerNotified && it.status != "pending" }
    }
    val wonUnnotifiedCount = remember(contests, user.uid) {
        contests.count { it.winnerCustomerId == user.uid && it.isPrizeApproved && !it.isWinnerNotified }
    }
    val unreadCount = unreadOrders.size + unreadTopUps.size + wonUnnotifiedCount

    if (role == UserRole.CUSTOMER && customerTab != 0) {
        BackHandler { customerTab = 0 }
    } else if (role == UserRole.ADMIN && adminTab != 0) {
        BackHandler { adminTab = 0 }
    }

    val customerTabs = listOf(
        NavTabItem("الرئيسية", Icons.Outlined.Home, "nav_home"),
        NavTabItem("الخدمات", Icons.Outlined.Storefront, "nav_services"),
        NavTabItem("العروض", Icons.Outlined.LocalOffer, "nav_offers"),
        NavTabItem("طلباتي", Icons.Outlined.ReceiptLong, "nav_orders"),
        NavTabItem("حسابي", Icons.Outlined.AccountCircle, "nav_account")
    )

    val adminTabs = listOf(
        NavTabItem("الإدارة", Icons.Outlined.AdminPanelSettings, "nav_admin_ops"),
        NavTabItem("الأرباح", Icons.Outlined.BarChart, "nav_admin_profits")
    )

    val activeTabs = when (role) {
        UserRole.CUSTOMER -> customerTabs
        UserRole.ADMIN -> adminTabs
        UserRole.MERCHANT -> emptyList()
    }
    val selectedTabIndex = when (role) {
        UserRole.CUSTOMER -> customerTab
        UserRole.ADMIN -> adminTab
        UserRole.MERCHANT -> 0
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp && activeTabs.isNotEmpty()

        Scaffold(
            containerColor = BgWhite,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TwinCashTopBar(
                    user = user,
                    unreadNotificationCount = unreadCount,
                    onNotificationClick = {
                        customerTab = 3
                        viewModel.markNotificationsRead()
                    },
                    onTopUpClick = if (role == UserRole.CUSTOMER) ({ showTopUpDialog = true }) else null,
                    onLogoutClick = onLogout
                )
            },
            bottomBar = {
                if (!useRail && activeTabs.isNotEmpty()) {
                    Column {
                        HorizontalDivider(thickness = 1.dp, color = BorderLight)
                        NavigationBar(
                            containerColor = BgWhite,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            activeTabs.forEachIndexed { idx, item ->
                                val selected = selectedTabIndex == idx
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (role == UserRole.CUSTOMER) {
                                            customerTab = idx
                                            if (idx != 1) serviceFilterCategory = null
                                        } else if (role == UserRole.ADMIN) {
                                            adminTab = idx
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            item.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NavyPrimary,
                                        selectedTextColor = NavyPrimary,
                                        indicatorColor = NavySoftContainer,
                                        unselectedIconColor = TextSecondarySlate,
                                        unselectedTextColor = TextSecondarySlate
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (role != UserRole.ADMIN) {
                    WhatsAppFab(
                        whatsappNumber = settings.whatsappNumber,
                        customerName = user.fullName
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = BgWhite) {
                        activeTabs.forEachIndexed { idx, item ->
                            val selected = selectedTabIndex == idx
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (role == UserRole.CUSTOMER) customerTab = idx
                                    else if (role == UserRole.ADMIN) adminTab = idx
                                },
                                icon = {
                                    Icon(item.icon, contentDescription = item.title)
                                },
                                label = { Text(item.title) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NavyPrimary,
                                    selectedTextColor = NavyPrimary,
                                    indicatorColor = NavySoftContainer,
                                    unselectedIconColor = TextSecondarySlate,
                                    unselectedTextColor = TextSecondarySlate
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (role) {
                        UserRole.CUSTOMER -> {
                            when (customerTab) {
                                0 -> HomeScreen(
                                    user = user,
                                    settings = settings,
                                    services = services,
                                    offers = offers,
                                    contests = contests,
                                    recentOrders = customerOrders,
                                    unreadTopUps = unreadTopUps,
                                    unreadOrders = unreadOrders,
                                    onNavigateToServices = { cat ->
                                        serviceFilterCategory = cat
                                        customerTab = 1
                                    },
                                    onNavigateToOffers = { customerTab = 2 },
                                    onNavigateToOrders = {
                                        customerTab = 3
                                        viewModel.markNotificationsRead()
                                    },
                                    onNavigateToAccount = { customerTab = 4 },
                                    onOpenTopUp = { showTopUpDialog = true },
                                    onDismissNotifications = viewModel::markNotificationsRead
                                )

                                1 -> ServicesScreen(
                                    user = user,
                                    settings = settings,
                                    services = services,
                                    initialCategory = serviceFilterCategory,
                                    isBusy = isBusy,
                                    onValidateCode = viewModel::validateDiscountCode,
                                    onPlaceOrder = viewModel::placeServiceOrder,
                                    onError = { viewModel.notify(it, isError = true) }
                                )

                                2 -> OffersScreen(
                                    user = user,
                                    settings = settings,
                                    offers = offers,
                                    isBusy = isBusy,
                                    onPlaceOfferOrder = viewModel::placeOfferOrder,
                                    onError = { viewModel.notify(it, isError = true) }
                                )

                                3 -> OrdersScreen(
                                    orders = customerOrders,
                                    topUps = customerTopUps,
                                    onOpenTopUp = { showTopUpDialog = true }
                                )

                                4 -> AccountScreen(
                                    user = user,
                                    settings = settings,
                                    services = services,
                                    discountCodes = customerDiscountCodes,
                                    contests = contests,
                                    myContestEntries = customerContestEntries,
                                    recentOrders = customerOrders,
                                    isBusy = isBusy,
                                    onUpdateProfile = viewModel::updateProfile,
                                    onValidateCode = viewModel::validateDiscountCode,
                                    onPlaceOrder = viewModel::placeServiceOrder,
                                    onJoinContest = viewModel::joinContest,
                                    onNavigateToOffers = { customerTab = 2 },
                                    onNavigateToOrders = { customerTab = 3 },
                                    onOpenTopUp = { showTopUpDialog = true },
                                    onNotify = viewModel::notify,
                                    onLogout = onLogout
                                )
                            }
                        }

                        UserRole.MERCHANT -> {
                            MerchantScreen(
                                merchant = user,
                                orders = merchantOrders,
                                isBusy = isBusy,
                                onAcceptOrder = viewModel::acceptAndCompleteOrder,
                                onRejectOrder = viewModel::rejectOrder
                            )
                        }

                        UserRole.ADMIN -> {
                            val profitSummary = remember(adminOrders, selectedProfitPeriod) {
                                viewModel.computeProfitSummary(adminOrders, selectedProfitPeriod)
                            }
                            if (adminTab == 0) {
                                AdminDashboardScreen(
                                    users = adminUsers,
                                    services = services,
                                    offers = offers,
                                    discountCodes = adminDiscountCodes,
                                    contests = contests,
                                    contestEntries = adminContestEntries,
                                    orders = adminOrders,
                                    topUps = adminTopUps,
                                    auditLogs = adminAuditLogs,
                                    settings = settings,
                                    todayProfit = profitSummary.todayProfit,
                                    isBusy = isBusy,
                                    onAcceptOrder = viewModel::acceptAndCompleteOrder,
                                    onRejectOrder = viewModel::rejectOrder,
                                    onApproveTopUp = viewModel::approveTopUp,
                                    onRejectTopUp = viewModel::rejectTopUp,
                                    onSaveService = viewModel::saveService,
                                    onToggleServiceAvailability = viewModel::toggleServiceAvailability,
                                    onDeleteService = viewModel::deleteService,
                                    onSaveOffer = viewModel::saveOffer,
                                    onDeleteOffer = viewModel::deleteOffer,
                                    onSaveDiscountCode = viewModel::saveDiscountCode,
                                    onToggleDiscountCode = viewModel::toggleDiscountCode,
                                    onDeleteDiscountCode = viewModel::deleteDiscountCode,
                                    onSaveContest = viewModel::saveContest,
                                    onSelectContestWinner = viewModel::selectContestWinner,
                                    onApproveContestPrize = viewModel::approveContestPrize,
                                    onDeleteContest = viewModel::deleteContest,
                                    onCreateMerchant = viewModel::createMerchant,
                                    onUpdateUserStatus = viewModel::updateUserStatus,
                                    onUpdateMerchantCommission = viewModel::updateMerchantCommission,
                                    onAdjustCustomerBalance = viewModel::adjustCustomerBalance,
                                    onUpdateSettings = viewModel::updateSettings
                                )
                            } else {
                                val serviceProfits = remember(adminOrders, selectedProfitPeriod) {
                                    viewModel.computeServiceProfits(adminOrders, selectedProfitPeriod)
                                }
                                val merchantProfits = remember(adminOrders, selectedProfitPeriod) {
                                    viewModel.computeMerchantProfits(adminOrders, selectedProfitPeriod)
                                }
                                val dailySeries = remember(adminOrders) {
                                    viewModel.computeDailySeries(adminOrders)
                                }
                                AdminProfitsScreen(
                                    orders = adminOrders,
                                    selectedPeriod = selectedProfitPeriod,
                                    summary = profitSummary,
                                    serviceProfits = serviceProfits,
                                    merchantProfits = merchantProfits,
                                    dailySeries = dailySeries,
                                    onSelectPeriod = viewModel::selectProfitPeriod,
                                    onBuildCsv = viewModel::buildProfitsCsvReport,
                                    onToast = { msg, err -> viewModel.notify(msg, err) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTopUpDialog && role == UserRole.CUSTOMER) {
        TopUpDialog(
            settings = settings,
            customerName = user.fullName,
            isBusy = isBusy,
            onDismiss = { showTopUpDialog = false },
            onSubmit = { amt, method, ref, uri ->
                viewModel.submitTopUp(amt, method, ref, uri) {
                    showTopUpDialog = false
                }
            },
            onCopiedPhone = {
                viewModel.notify("تم نسخ رقم الوكالة ${settings.agencyPhone}")
            }
        )
    }
}
