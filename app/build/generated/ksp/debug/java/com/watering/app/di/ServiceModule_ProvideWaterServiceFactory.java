package com.watering.app.di;

import android.content.Context;
import com.watering.app.core.data.WaterRepository;
import com.watering.app.core.service.WaterService;
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
public final class ServiceModule_ProvideWaterServiceFactory implements Factory<WaterService> {
  private final Provider<Context> contextProvider;

  private final Provider<WaterRepository> repositoryProvider;

  private final Provider<WateringWidgetUpdater> updaterProvider;

  public ServiceModule_ProvideWaterServiceFactory(Provider<Context> contextProvider,
      Provider<WaterRepository> repositoryProvider,
      Provider<WateringWidgetUpdater> updaterProvider) {
    this.contextProvider = contextProvider;
    this.repositoryProvider = repositoryProvider;
    this.updaterProvider = updaterProvider;
  }

  @Override
  public WaterService get() {
    return provideWaterService(contextProvider.get(), repositoryProvider.get(), updaterProvider.get());
  }

  public static ServiceModule_ProvideWaterServiceFactory create(Provider<Context> contextProvider,
      Provider<WaterRepository> repositoryProvider,
      Provider<WateringWidgetUpdater> updaterProvider) {
    return new ServiceModule_ProvideWaterServiceFactory(contextProvider, repositoryProvider, updaterProvider);
  }

  public static WaterService provideWaterService(Context context, WaterRepository repository,
      WateringWidgetUpdater updater) {
    return Preconditions.checkNotNullFromProvides(ServiceModule.INSTANCE.provideWaterService(context, repository, updater));
  }
}
