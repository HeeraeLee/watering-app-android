package com.watering.app.core.service;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class MidnightResetWorker_Factory {
  private final Provider<WaterService> waterServiceProvider;

  private final Provider<NotificationService> notificationServiceProvider;

  public MidnightResetWorker_Factory(Provider<WaterService> waterServiceProvider,
      Provider<NotificationService> notificationServiceProvider) {
    this.waterServiceProvider = waterServiceProvider;
    this.notificationServiceProvider = notificationServiceProvider;
  }

  public MidnightResetWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, waterServiceProvider.get(), notificationServiceProvider.get());
  }

  public static MidnightResetWorker_Factory create(Provider<WaterService> waterServiceProvider,
      Provider<NotificationService> notificationServiceProvider) {
    return new MidnightResetWorker_Factory(waterServiceProvider, notificationServiceProvider);
  }

  public static MidnightResetWorker newInstance(Context context, WorkerParameters params,
      WaterService waterService, NotificationService notificationService) {
    return new MidnightResetWorker(context, params, waterService, notificationService);
  }
}
