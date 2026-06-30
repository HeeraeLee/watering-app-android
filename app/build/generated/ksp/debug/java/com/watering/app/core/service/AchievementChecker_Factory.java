package com.watering.app.core.service;

import com.watering.app.core.datastore.AchievementDataStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AchievementChecker_Factory implements Factory<AchievementChecker> {
  private final Provider<AchievementDataStore> dataStoreProvider;

  public AchievementChecker_Factory(Provider<AchievementDataStore> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public AchievementChecker get() {
    return newInstance(dataStoreProvider.get());
  }

  public static AchievementChecker_Factory create(
      Provider<AchievementDataStore> dataStoreProvider) {
    return new AchievementChecker_Factory(dataStoreProvider);
  }

  public static AchievementChecker newInstance(AchievementDataStore dataStore) {
    return new AchievementChecker(dataStore);
  }
}
