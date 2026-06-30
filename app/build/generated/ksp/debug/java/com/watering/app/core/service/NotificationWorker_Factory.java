package com.watering.app.core.service;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.watering.app.core.data.SettingsRepository;
import com.watering.app.core.data.WaterRepository;
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
public final class NotificationWorker_Factory {
  private final Provider<WaterRepository> waterRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public NotificationWorker_Factory(Provider<WaterRepository> waterRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.waterRepositoryProvider = waterRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  public NotificationWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, waterRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static NotificationWorker_Factory create(Provider<WaterRepository> waterRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new NotificationWorker_Factory(waterRepositoryProvider, settingsRepositoryProvider);
  }

  public static NotificationWorker newInstance(Context context, WorkerParameters params,
      WaterRepository waterRepository, SettingsRepository settingsRepository) {
    return new NotificationWorker(context, params, waterRepository, settingsRepository);
  }
}
