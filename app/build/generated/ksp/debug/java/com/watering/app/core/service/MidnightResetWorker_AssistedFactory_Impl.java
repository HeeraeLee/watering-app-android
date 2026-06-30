package com.watering.app.core.service;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MidnightResetWorker_AssistedFactory_Impl implements MidnightResetWorker_AssistedFactory {
  private final MidnightResetWorker_Factory delegateFactory;

  MidnightResetWorker_AssistedFactory_Impl(MidnightResetWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public MidnightResetWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<MidnightResetWorker_AssistedFactory> create(
      MidnightResetWorker_Factory delegateFactory) {
    return InstanceFactory.create(new MidnightResetWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<MidnightResetWorker_AssistedFactory> createFactoryProvider(
      MidnightResetWorker_Factory delegateFactory) {
    return InstanceFactory.create(new MidnightResetWorker_AssistedFactory_Impl(delegateFactory));
  }
}
