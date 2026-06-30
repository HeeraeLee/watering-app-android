package com.watering.app.di;

import android.content.Context;
import com.watering.app.core.service.NotificationService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ServiceModule_ProvideNotificationServiceFactory implements Factory<NotificationService> {
  private final Provider<Context> contextProvider;

  public ServiceModule_ProvideNotificationServiceFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NotificationService get() {
    return provideNotificationService(contextProvider.get());
  }

  public static ServiceModule_ProvideNotificationServiceFactory create(
      Provider<Context> contextProvider) {
    return new ServiceModule_ProvideNotificationServiceFactory(contextProvider);
  }

  public static NotificationService provideNotificationService(Context context) {
    return Preconditions.checkNotNullFromProvides(ServiceModule.INSTANCE.provideNotificationService(context));
  }
}
