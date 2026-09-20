package com.attendance.app.di

import android.content.Context
import com.attendance.app.facerecognition.FaceEmbedder
import com.attendance.app.facerecognition.MlKitFaceDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FaceRecognitionModule {

    @Provides
    @Singleton
    fun provideMlKitFaceDetector(): MlKitFaceDetector = MlKitFaceDetector()

    @Provides
    @Singleton
    fun provideFaceEmbedder(@ApplicationContext context: Context): FaceEmbedder = FaceEmbedder(context)
}
