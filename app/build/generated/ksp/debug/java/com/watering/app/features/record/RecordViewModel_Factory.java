package com.watering.app.features.record;

import com.watering.app.core.data.SettingsRepository;
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
public final class RecordViewModel_Factory implements Factory<RecordViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public RecordViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public RecordViewModel get() {
    return newInstance(settingsRepositoryProvider.get());
  }

  public static RecordViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new RecordViewModel_Factory(settingsRepositoryProvider);
  }

  public static RecordViewModel newInstance(SettingsRepository settingsRepository) {
    return new RecordViewModel(settingsRepository);
  }
}
