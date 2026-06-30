package com.watering.app.core.datastore;

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
public final class WaterDataStore_Factory implements Factory<WaterDataStore> {
  private final Provider<Context> contextProvider;

  public WaterDataStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WaterDataStore get() {
    return newInstance(contextProvider.get());
  }

  public static WaterDataStore_Factory create(Provider<Context> contextProvider) {
    return new WaterDataStore_Factory(contextProvider);
  }

  public static WaterDataStore newInstance(Context context) {
    return new WaterDataStore(context);
  }
}
