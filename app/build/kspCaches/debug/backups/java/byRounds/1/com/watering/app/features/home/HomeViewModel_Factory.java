package com.watering.app.features.home;

import com.watering.app.core.data.SettingsRepository;
import com.watering.app.core.data.WaterRepository;
import com.watering.app.core.service.AchievementChecker;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<WaterService> waterServiceProvider;

  private final Provider<WaterRepository> waterRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<AchievementChecker> achievementCheckerProvider;

  public HomeViewModel_Factory(Provider<WaterService> waterServiceProvider,
      Provider<WaterRepository> waterRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AchievementChecker> achievementCheckerProvider) {
    this.waterServiceProvider = waterServiceProvider;
    this.waterRepositoryProvider = waterRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.achievementCheckerProvider = achievementCheckerProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(waterServiceProvider.get(), waterRepositoryProvider.get(), settingsRepositoryProvider.get(), achievementCheckerProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<WaterService> waterServiceProvider,
      Provider<WaterRepository> waterRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AchievementChecker> achievementCheckerProvider) {
    return new HomeViewModel_Factory(waterServiceProvider, waterRepositoryProvider, settingsRepositoryProvider, achievementCheckerProvider);
  }

  public static HomeViewModel newInstance(WaterService waterService,
      WaterRepository waterRepository, SettingsRepository settingsRepository,
      AchievementChecker achievementChecker) {
    return new HomeViewModel(waterService, waterRepository, settingsRepository, achievementChecker);
  }
}
