package ru.nstu.yopta.ast;

/** Объявление верхнего уровня: {@code interface} или {@code type}. */
public sealed interface AstDeclaration permits InterfaceDeclaration, TypeAliasDeclaration {
}
