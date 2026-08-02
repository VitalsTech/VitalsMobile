package com.vitals.mobile.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import com.vitals.mobile.feature.aiassistant.AiAssistantScreen
import com.vitals.mobile.feature.consultations.ConsultationDetailScreen
import com.vitals.mobile.feature.consultations.MyConsultationsScreen
import com.vitals.mobile.feature.doctors.DoctorBookScreen
import com.vitals.mobile.feature.doctors.DoctorChatScreen
import com.vitals.mobile.feature.doctors.DoctorDetailScreen
import com.vitals.mobile.feature.doctors.DoctorsListScreen
import com.vitals.mobile.feature.documents.DocumentDetailScreen
import com.vitals.mobile.feature.documents.DocumentNewScreen
import com.vitals.mobile.feature.documents.DocumentsScreen
import com.vitals.mobile.feature.labs.LabsScreen
import com.vitals.mobile.feature.misc.HouseCallScreen
import com.vitals.mobile.feature.misc.MoreScreen
import com.vitals.mobile.feature.misc.NotificationsScreen
import com.vitals.mobile.feature.misc.SupportScreen
import com.vitals.mobile.feature.overview.MedicalOverviewScreen
import com.vitals.mobile.feature.path.PathScreen
import com.vitals.mobile.feature.prescriptions.PrescriptionDetailScreen
import com.vitals.mobile.feature.profile.ProfileEditScreen
import com.vitals.mobile.feature.profile.ProfileScreen
import com.vitals.mobile.feature.treatment.TreatmentScreen
import com.vitals.mobile.feature.triage.TriageOnboardingScreen
import com.vitals.mobile.feature.triage.TriageResultScreen

fun NavGraphBuilder.vitalsNavGraph(navController: NavHostController) {

    composable(NavRoutes.PATH) {
        PathScreen(navController = navController)
    }

    composable(NavRoutes.TRIAGE_CHAT) {
        AiAssistantScreen(navController = navController)
    }

    composable(NavRoutes.DOCTORS) {
        DoctorsListScreen(navController = navController)
    }

    composable(NavRoutes.PROFILE) {
        ProfileScreen(navController = navController)
    }

    composable(NavRoutes.MORE) {
        MoreScreen(navController = navController)
    }

    composable(NavRoutes.TRIAGE_ONBOARDING) {
        TriageOnboardingScreen(navController = navController)
    }

    composable(
        route = NavRoutes.TRIAGE_RESULT,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
        TriageResultScreen(sessionId = sessionId, navController = navController)
    }

    composable(
        route = NavRoutes.DOCTOR_DETAIL,
        arguments = listOf(navArgument("doctorId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val doctorId = backStackEntry.arguments?.getString("doctorId").orEmpty()
        DoctorDetailScreen(doctorId = doctorId, navController = navController)
    }

    composable(
        route = NavRoutes.DOCTOR_BOOK,
        arguments = listOf(navArgument("doctorId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val doctorId = backStackEntry.arguments?.getString("doctorId").orEmpty()
        DoctorBookScreen(doctorId = doctorId, navController = navController)
    }

    composable(
        route = NavRoutes.DOCTOR_CHAT,
        arguments = listOf(navArgument("doctorId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val doctorId = backStackEntry.arguments?.getString("doctorId").orEmpty()
        DoctorChatScreen(doctorId = doctorId, navController = navController)
    }

    composable(NavRoutes.PROFILE_EDIT) {
        ProfileEditScreen(navController = navController)
    }

    composable(NavRoutes.DOCUMENTS) {
        DocumentsScreen(navController = navController)
    }

    composable(NavRoutes.DOCUMENT_NEW) {
        DocumentNewScreen(navController = navController)
    }

    composable(
        route = NavRoutes.DOCUMENT_DETAIL,
        arguments = listOf(navArgument("documentId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val documentId = backStackEntry.arguments?.getString("documentId").orEmpty()
        DocumentDetailScreen(documentId = documentId, navController = navController)
    }

    composable(NavRoutes.CONSULTATIONS) {
        MyConsultationsScreen(navController = navController)
    }

    composable(
        route = NavRoutes.CONSULTATION_DETAIL,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
        ConsultationDetailScreen(sessionId = sessionId, navController = navController)
    }

    composable(NavRoutes.TREATMENT) {
        TreatmentScreen(navController = navController)
    }

    composable(NavRoutes.LABS) {
        LabsScreen(navController = navController)
    }

    composable(
        route = NavRoutes.PRESCRIPTION_DETAIL,
        arguments = listOf(navArgument("prescriptionId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val prescriptionId = backStackEntry.arguments?.getString("prescriptionId").orEmpty()
        PrescriptionDetailScreen(prescriptionId = prescriptionId, navController = navController)
    }

    composable(NavRoutes.MEDICAL_OVERVIEW) {
        MedicalOverviewScreen(navController = navController)
    }

    composable(NavRoutes.NOTIFICATIONS) {
        NotificationsScreen(navController = navController)
    }

    composable(NavRoutes.SUPPORT) {
        SupportScreen(navController = navController)
    }

    composable(NavRoutes.HOUSE_CALL) {
        HouseCallScreen(navController = navController)
    }
}
