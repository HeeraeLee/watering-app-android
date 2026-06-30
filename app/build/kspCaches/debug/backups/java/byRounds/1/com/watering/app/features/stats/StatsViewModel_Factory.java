package com.watering.app.features.stats;

import com.watering.app.core.data.SettingsRepository;
import com.watering.app.core.data.WaterRepository;
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
public final class StatsViewModel_Factory implements Factory<StatsViewModel> {
  private final Provider<WaterRepository> waterRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public StatsViewModel_Factory(Provider<WaterRepository> waterRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.waterRepositoryProvider = waterRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public StatsViewModel get() {
    return newInstance(waterRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static StatsViewModel_Factory create(Provider<WaterRepository> waterRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new StatsViewModel_Factory(waterRepositoryProvider, settingsRepositoryProvider);
  }

  public static StatsViewModel newInstance(WaterRepository waterRepository,
      SettingsRepository settingsRepository) {
    return new StatsViewModel(waterRepository, settingsRepository);
  }
}
