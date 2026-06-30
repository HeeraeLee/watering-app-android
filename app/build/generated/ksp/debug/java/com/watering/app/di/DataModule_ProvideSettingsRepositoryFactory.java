package com.watering.app.di;

import com.watering.app.core.data.SettingsRepository;
import com.watering.app.core.datastore.SettingsDataStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DataModule_ProvideSettingsRepositoryFactory implements Factory<SettingsRepository> {
  private final Provider<SettingsDataStore> dataStoreProvider;

  public DataModule_ProvideSettingsRepositoryFactory(
      Provider<SettingsDataStore> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public SettingsRepository get() {
    return provideSettingsRepository(dataStoreProvider.get());
  }

  public static DataModule_ProvideSettingsRepositoryFactory create(
      Provider<SettingsDataStore> dataStoreProvider) {
    return new DataModule_ProvideSettingsRepositoryFactory(dataStoreProvider);
  }

  public static SettingsRepository provideSettingsRepository(SettingsDataStore dataStore) {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideSettingsRepository(dataStore));
  }
}
