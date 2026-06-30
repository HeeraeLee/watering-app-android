package com.watering.app.features.settings;

import com.watering.app.core.data.SettingsRepository;
import com.watering.app.core.service.NotificationService;
import com.watering.app.core.service.WaterService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<NotificationService> notificationServiceProvider;

  private final Provider<WaterService> waterServiceProvider;

  public SettingsViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<NotificationService> notificationServiceProvider,
      Provider<WaterService> waterServiceProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.notificationServiceProvider = notificationServiceProvider;
    this.waterServiceProvider = waterServiceProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsRepositoryProvider.get(), notificationServiceProvider.get(), waterServiceProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<NotificationService> notificationServiceProvider,
      Provider<WaterService> waterServiceProvider) {
    return new SettingsViewModel_Factory(settingsRepositoryProvider, notificationServiceProvider, waterServiceProvider);
  }

  public static SettingsViewModel newInstance(SettingsRepository settingsRepository,
      NotificationService notificationService, WaterService waterService) {
    return new SettingsViewModel(settingsRepository, notificationService, waterService);
  }
}
