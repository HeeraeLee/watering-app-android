package com.watering.app.di;

import android.content.Context;
import com.watering.app.widget.WateringWidgetUpdater;
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
public final class ServiceModule_ProvideWateringWidgetUpdaterFactory implements Factory<WateringWidgetUpdater> {
  private final Provider<Context> contextProvider;

  public ServiceModule_ProvideWateringWidgetUpdaterFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WateringWidgetUpdater get() {
    return provideWateringWidgetUpdater(contextProvider.get());
  }

  public static ServiceModule_ProvideWateringWidgetUpdaterFactory create(
      Provider<Context> contextProvider) {
    return new ServiceModule_ProvideWateringWidgetUpdaterFactory(contextProvider);
  }

  public static WateringWidgetUpdater provideWateringWidgetUpdater(Context context) {
    return Preconditions.checkNotNullFromProvides(ServiceModule.INSTANCE.provideWateringWidgetUpdater(context));
  }
}
