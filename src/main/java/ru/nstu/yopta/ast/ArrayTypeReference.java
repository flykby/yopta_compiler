package ru.nstu.yopta.ast;

/** Массив: элементный тип + суффикс {@code []}. */
public final class ArrayTypeReference implements TypeReference {

    private final TypeReference elementType;

    public ArrayTypeReference(TypeReference elementType) {
        this.elementType = elementType;
    }

    public TypeReference getElementType() {
        return elementType;
    }
}
