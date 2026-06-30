package com.watering.app.features.onboarding;

import com.watering.app.core.data.SettingsRepository;
import com.watering.app.core.service.NotificationService;
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
public final class OnboardingViewModel_Factory implements Factory<OnboardingViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<NotificationService> notificationServiceProvider;

  public OnboardingViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<NotificationService> notificationServiceProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.notificationServiceProvider = notificationServiceProvider;
  }

  @Override
  public OnboardingViewModel get() {
    return newInstance(settingsRepositoryProvider.get(), notificationServiceProvider.get());
  }

  public static OnboardingViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<NotificationService> notificationServiceProvider) {
    return new OnboardingViewModel_Factory(settingsRepositoryProvider, notificationServiceProvider);
  }

  public static OnboardingViewModel newInstance(SettingsRepository settingsRepository,
      NotificationService notificationService) {
    return new OnboardingViewModel(settingsRepository, notificationService);
  }
}
