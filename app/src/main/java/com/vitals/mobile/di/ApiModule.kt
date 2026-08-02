package com.vitals.mobile.di

import com.vitals.mobile.core.data.auth.AuthApi
import com.vitals.mobile.core.data.consultations.ConsultationsApi
import com.vitals.mobile.core.data.doctors.DoctorsApi
import com.vitals.mobile.core.data.laborders.LabOrdersApi
import com.vitals.mobile.core.data.medicalrecords.MedicalRecordsApi
import com.vitals.mobile.core.data.notifications.NotificationsApi
import com.vitals.mobile.core.data.prescriptions.PrescriptionsApi
import com.vitals.mobile.core.data.routing.RoutingApi
import com.vitals.mobile.core.data.triage.TriageApi
import com.vitals.mobile.core.data.users.UsersApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideUsersApi(retrofit: Retrofit): UsersApi = retrofit.create(UsersApi::class.java)

    @Provides
    @Singleton
    fun provideDoctorsApi(retrofit: Retrofit): DoctorsApi = retrofit.create(DoctorsApi::class.java)

    @Provides
    @Singleton
    fun provideTriageApi(retrofit: Retrofit): TriageApi = retrofit.create(TriageApi::class.java)

    @Provides
    @Singleton
    fun provideConsultationsApi(retrofit: Retrofit): ConsultationsApi = retrofit.create(ConsultationsApi::class.java)

    @Provides
    @Singleton
    fun provideMedicalRecordsApi(retrofit: Retrofit): MedicalRecordsApi = retrofit.create(MedicalRecordsApi::class.java)

    @Provides
    @Singleton
    fun providePrescriptionsApi(retrofit: Retrofit): PrescriptionsApi = retrofit.create(PrescriptionsApi::class.java)

    @Provides
    @Singleton
    fun provideLabOrdersApi(retrofit: Retrofit): LabOrdersApi = retrofit.create(LabOrdersApi::class.java)

    @Provides
    @Singleton
    fun provideRoutingApi(retrofit: Retrofit): RoutingApi = retrofit.create(RoutingApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationsApi(retrofit: Retrofit): NotificationsApi = retrofit.create(NotificationsApi::class.java)
}
