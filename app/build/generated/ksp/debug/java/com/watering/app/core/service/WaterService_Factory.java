package com.watering.app.core.service;

import android.content.Context;
import com.watering.app.core.data.WaterRepository;
import com.watering.app.widget.WateringWidgetUpdater;
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
public final class WaterService_Factory implements Factory<WaterService> {
  private final Provider<Context> contextProvider;

  private final Provider<WaterRepository> repositoryProvider;

  private final Provider<WateringWidgetUpdater> widgetUpdaterProvider;

  public WaterService_Factory(Provider<Context> contextProvider,
      Provider<WaterRepository> repositoryProvider,
      Provider<WateringWidgetUpdater> widgetUpdaterProvider) {
    this.contextProvider = contextProvider;
    this.repositoryProvider = repositoryProvider;
    this.widgetUpdaterProvider = widgetUpdaterProvider;
  }

  @Override
  public WaterService get() {
    return newInstance(contextProvider.get(), repositoryProvider.get(), widgetUpdaterProvider.get());
  }

  public static WaterService_Factory create(Provider<Context> contextProvider,
      Provider<WaterRepository> repositoryProvider,
      Provider<WateringWidgetUpdater> widgetUpdaterProvider) {
    return new WaterService_Factory(contextProvider, repositoryProvider, widgetUpdaterProvider);
  }

  public static WaterService newInstance(Context context, WaterRepository repository,
      WateringWidgetUpdater widgetUpdater) {
    return new WaterService(context, repository, widgetUpdater);
  }
}
