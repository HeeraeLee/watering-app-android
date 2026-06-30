package com.watering.app.core.service;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class NotificationService_Factory implements Factory<NotificationService> {
  private final Provider<Context> contextProvider;

  public NotificationService_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NotificationService get() {
    return newInstance(contextProvider.get());
  }

  public static NotificationService_Factory create(Provider<Context> contextProvider) {
    return new NotificationService_Factory(contextProvider);
  }

  public static NotificationService newInstance(Context context) {
    return new NotificationService(context);
  }
}
