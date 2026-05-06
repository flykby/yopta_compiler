package ru.nstu.yopta.ast;

/** Ссылка на тип: имя ({@link NamedTypeReference}) или массив {@code T[]}. */
public sealed interface TypeReference permits NamedTypeReference, ArrayTypeReference {
}
