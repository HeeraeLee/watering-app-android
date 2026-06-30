package com.watering.app;

import androidx.hilt.work.HiltWorkerFactory;
import com.watering.app.core.service.NotificationService;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class WateringApp_MembersInjector implements MembersInjector<WateringApp> {
  private final Provider<NotificationService> notificationServiceProvider;

  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public WateringApp_MembersInjector(Provider<NotificationService> notificationServiceProvider,
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.notificationServiceProvider = notificationServiceProvider;
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<WateringApp> create(
      Provider<NotificationService> notificationServiceProvider,
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new WateringApp_MembersInjector(notificationServiceProvider, workerFactoryProvider);
  }

  @Override
  public void injectMembers(WateringApp instance) {
    injectNotificationService(instance, notificationServiceProvider.get());
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.watering.app.WateringApp.notificationService")
  public static void injectNotificationService(WateringApp instance,
      NotificationService notificationService) {
    instance.notificationService = notificationService;
  }

  @InjectedFieldSignature("com.watering.app.WateringApp.workerFactory")
  public static void injectWorkerFactory(WateringApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
