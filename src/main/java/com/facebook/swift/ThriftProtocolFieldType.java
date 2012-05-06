/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import static com.facebook.swift.metadata.TypeParameterUtils.getRawType;

public enum ThriftProtocolFieldType {
  UNKNOWN((byte) 0),
  BOOL((byte) 2, boolean.class),
  BYTE((byte) 3, byte.class),
  DOUBLE((byte) 4, double.class),
  I16((byte) 6, short.class),
  I32((byte) 8, int.class),
  I64((byte) 10, long.class),
  STRING((byte) 11, ByteBuffer.class),
  STRUCT((byte) 12, null),
  MAP((byte) 13, null),
  SET((byte) 14, null),
  LIST((byte) 15, null),
  ENUM((byte) 8, null); // same as I32 type

  private final byte type;
  private final Class<?> defaultJavaType;

  private ThriftProtocolFieldType(byte type, Class<?> defaultJavaType) {
    this.type = type;
    this.defaultJavaType = defaultJavaType;
  }

  private ThriftProtocolFieldType(byte type) {
    this.type = type;
    defaultJavaType = null;
  }

  public Class<?> getDefaultJavaType() {
    return defaultJavaType;
  }

  public boolean isJavaPrimitive() {
    return defaultJavaType != null && defaultJavaType.isPrimitive();
  }

  public byte getType() {
    return type;
  }

  private static Map<Class<?>, ThriftProtocolFieldType> typesByClass;

  static {
    ImmutableMap.Builder<Class<?>, ThriftProtocolFieldType> builder = ImmutableMap.builder();
    for (ThriftProtocolFieldType protocolType : ThriftProtocolFieldType.values()) {
      if (protocolType.defaultJavaType != null) {
        builder.put(protocolType.defaultJavaType, protocolType);
      }
    }
    typesByClass = builder.build();
  }

  public static boolean isSupportedJavaType(Type genericType) {
    Class<?> rawType = getRawType(genericType);
    return Enum.class.isAssignableFrom(rawType) ||
        Map.class.isAssignableFrom(rawType) ||
        Iterable.class.isAssignableFrom(rawType) ||
        rawType.isAnnotationPresent(ThriftStruct.class) ||
        typesByClass.containsKey(rawType);
  }

  public static ThriftProtocolFieldType inferProtocolType(Type genericType) {
    Class<?> rawType = getRawType(genericType);

    ThriftProtocolFieldType protocolType = typesByClass.get(rawType);
    if (protocolType != null) {
      return protocolType;
    }
    if (Enum.class.isAssignableFrom(rawType)) {
      return ENUM;
    }
    if (Map.class.isAssignableFrom(rawType)) {
      return MAP;
    }
    if (Set.class.isAssignableFrom(rawType)) {
      return SET;
    }
    if (Iterable.class.isAssignableFrom(rawType)) {
      return LIST;
    }
    if (void.class.isAssignableFrom(rawType)) {
      return STRUCT;
    }
    if (rawType.isAnnotationPresent(ThriftStruct.class)) {
      return STRUCT;
    }

    return null;
  }
}
