package io.kroki.server.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SafeModeTest {

  @Test
  void should_return_default_value() {
    assertThat(SafeMode.get("yolo", SafeMode.UNSAFE)).isEqualTo(SafeMode.UNSAFE);
    assertThat(SafeMode.get("yolo", SafeMode.SAFE)).isEqualTo(SafeMode.SAFE);
    assertThat(SafeMode.get("yolo", SafeMode.SECURE)).isEqualTo(SafeMode.SECURE);
  }

  @Test
  void should_get_safe_mode_case_insensitive() {
    assertThat(SafeMode.get("unsafe", null)).isEqualTo(SafeMode.UNSAFE);
    assertThat(SafeMode.get("Unsafe", null)).isEqualTo(SafeMode.UNSAFE);
    assertThat(SafeMode.get("UNSAFE", null)).isEqualTo(SafeMode.UNSAFE);
    assertThat(SafeMode.get("safe", null)).isEqualTo(SafeMode.SAFE);
    assertThat(SafeMode.get("Safe", null)).isEqualTo(SafeMode.SAFE);
    assertThat(SafeMode.get("SAFE", null)).isEqualTo(SafeMode.SAFE);
    assertThat(SafeMode.get("secure", null)).isEqualTo(SafeMode.SECURE);
    assertThat(SafeMode.get("SeCuRE", null)).isEqualTo(SafeMode.SECURE);
    assertThat(SafeMode.get("SECURE", null)).isEqualTo(SafeMode.SECURE);
  }

  @Test
  void should_be_null_safe() {
    assertThat(SafeMode.get(null, SafeMode.SECURE)).isEqualTo(SafeMode.SECURE);
    assertThat(SafeMode.get("", SafeMode.SECURE)).isEqualTo(SafeMode.SECURE);
    assertThat(SafeMode.get("   ", SafeMode.SECURE)).isEqualTo(SafeMode.SECURE);
  }

  @Test
  void safe_mode_are_comparable() {
    assertThat(SafeMode.UNSAFE.value).isLessThan(SafeMode.SAFE.value);
    assertThat(SafeMode.UNSAFE.value).isLessThan(SafeMode.SECURE.value);
    assertThat(SafeMode.SAFE.value).isLessThan(SafeMode.SECURE.value);
  }
}
