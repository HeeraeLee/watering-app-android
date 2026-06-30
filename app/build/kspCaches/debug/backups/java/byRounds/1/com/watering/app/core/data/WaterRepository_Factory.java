package com.watering.app.core.data;

import com.watering.app.core.datastore.WaterDataStore;
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
public final class WaterRepository_Factory implements Factory<WaterRepository> {
  private final Provider<WaterDataStore> dataStoreProvider;

  public WaterRepository_Factory(Provider<WaterDataStore> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public WaterRepository get() {
    return newInstance(dataStoreProvider.get());
  }

  public static WaterRepository_Factory create(Provider<WaterDataStore> dataStoreProvider) {
    return new WaterRepository_Factory(dataStoreProvider);
  }

  public static WaterRepository newInstance(WaterDataStore dataStore) {
    return new WaterRepository(dataStore);
  }
}
