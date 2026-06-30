package com.watering.app.di;

import com.watering.app.core.data.WaterRepository;
import com.watering.app.core.datastore.WaterDataStore;
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
public final class DataModule_ProvideWaterRepositoryFactory implements Factory<WaterRepository> {
  private final Provider<WaterDataStore> dataStoreProvider;

  public DataModule_ProvideWaterRepositoryFactory(Provider<WaterDataStore> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public WaterRepository get() {
    return provideWaterRepository(dataStoreProvider.get());
  }

  public static DataModule_ProvideWaterRepositoryFactory create(
      Provider<WaterDataStore> dataStoreProvider) {
    return new DataModule_ProvideWaterRepositoryFactory(dataStoreProvider);
  }

  public static WaterRepository provideWaterRepository(WaterDataStore dataStore) {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideWaterRepository(dataStore));
  }
}
