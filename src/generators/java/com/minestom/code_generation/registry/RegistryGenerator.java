package com.minestom.code_generation.registry;

import com.minestom.code_generation.MinestomCodeGenerator;
import com.squareup.javapoet.*;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RegistryGenerator extends MinestomCodeGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryGenerator.class);
    // Map of registry defaults
    // MainTypeClassName - DefaultValue - RegistryClassName
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final Triple<ClassName, String, ClassName>[] registries = new Triple[]{
            new Triple(ClassName.get("net.minestom.server.instance.block", "Block"), "AIR", ClassName.get("net.minestom.server.registry", "IdCrossMapRegistry", "Defaulted")),
            new Triple(ClassName.get("net.minestom.server.fluid", "Fluid"), "EMPTY", ClassName.get("net.minestom.server.registry", "IdCrossMapRegistry", "Defaulted")),
            new Triple(ClassName.get("net.minestom.server.item", "Material"), "AIR", ClassName.get("net.minestom.server.registry", "IdCrossMapRegistry", "Defaulted")),
            new Triple(ClassName.get("net.minestom.server.item", "Enchantment"), null, ClassName.get("net.minestom.server.registry", "IdCrossMapRegistry")),
            new Triple(ClassName.get("net.minestom.server.attribute", "Attribute"), null, ClassName.get("net.minestom.server.registry", "MapRegistry")),
            new Triple(ClassName.get("net.minestom.server.entity", "EntityType"), null, ClassName.get("net.minestom.server.registry", "IdCrossMapRegistry")),
            new Triple(ClassName.get("net.minestom.server.entity.metadata.villager", "VillagerProfession"), null, ClassName.get("net.minestom.server.registry", "MapRegistry")),
            new Triple(ClassName.get("net.minestom.server.entity.metadata.villager", "VillagerType"), null, ClassName.get("net.minestom.server.registry", "MapRegistry")),
            new Triple(ClassName.get("net.minestom.server.particle", "Particle"), null, ClassName.get("net.minestom.server.registry", "IdCrossMapRegistry")),
            new Triple(ClassName.get("net.minestom.server.potion", "PotionType"), "EMPTY", ClassName.get("net.minestom.server.registry", "MapRegistry", "Defaulted")),
            new Triple(ClassName.get("net.minestom.server.potion", "PotionEffect"), null, ClassName.get("net.minestom.server.registry", "IdCrossMapRegistry")),
            new Triple(ClassName.get("net.minestom.server.sound", "SoundEvent"), null, ClassName.get("net.minestom.server.registry", "IdCrossMapRegistry")),
            new Triple(ClassName.get("net.minestom.server.statistic", "StatisticType"), null, ClassName.get("net.minestom.server.registry", "IdCrossMapRegistry"))
    };
    private final File outputFolder;

    public RegistryGenerator() {
        this(null);
    }

    public RegistryGenerator(@Nullable File outputFolder) {
        this.outputFolder = Objects.requireNonNullElse(outputFolder, DEFAULT_OUTPUT_FOLDER);

    }

    @Override
    public void generate() {
        // Important classes we use alot
        ClassName namespaceIDClassName = ClassName.get("net.minestom.server.utils", "NamespaceID");
        ClassName keyIDClassName = ClassName.get("net.kyori.adventure.key", "Key");

        // Registry class
        ClassName registeriesClassName = ClassName.get("net.minestom.server.registry", "Registries");

        TypeSpec.Builder registriesClass = TypeSpec.classBuilder(registeriesClassName)
                .addModifiers(Modifier.PUBLIC).addModifiers(Modifier.FINAL)
                .addJavadoc("AUTOGENERATED");

        FieldSpec[] registryFields = new FieldSpec[registries.length];
        // Generate Registries
        for (int i = 0; i < registries.length; i++) {
            ClassName type = registries[i].getFirst();
            String defaultValue = registries[i].getSecond();
            ClassName registryType = registries[i].getThird();
            CodeBlock init;
            if (defaultValue != null) {
                init = CodeBlock.builder()
                        .addStatement(
                                "new $T<>($T.$N)",
                                registryType,
                                type,
                                defaultValue
                        )
                        .build();
            } else {
                init = CodeBlock.builder()
                        .addStatement(
                                "new $T<>()",
                                registryType
                        )
                        .build();
            }
            FieldSpec registryField = FieldSpec.builder(
                    ParameterizedTypeName.get(
                            registryType,
                            type
                    ),
                    decapitalizeString(type.simpleName()) + "Registry" // e.g. blockRegistry, potionEffectRegistry
            )
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(init)
                    .build();

            registryFields[i] = registryField;
            registriesClass.addField(registryField);
        }
        // Generate methods
/*
            // Get examples:
            /** Returns 'AIR' if none match
            public static Block getBlock(String id) {
                return getBlock(NamespaceID.from(id));
            }

            /** Returns 'AIR' if none match
            public static Block getBlock(NamespaceID id) {
                return blocks.getOrDefault(id, AIR);
            }
            // Add examples:
            public static boolean addBlock(NamespaceID id, Block block) {
                // ensure the block is not already there (map.values.contains()) (return false if already there)
                // ensure the namespace is not already there (map.keySet.contains()) (return false if already there)
                // add to block registry hashmap. (return true)
            }
 */

        for (int i = 0; i < registries.length; i++) {
            ClassName type = registries[i].getFirst();
            String defaultValue = registries[i].getSecond();
            ClassName registryType = registries[i].getThird();
            FieldSpec registryField = registryFields[i];
            String typeName = type.simpleName();

            ParameterSpec namespaceIDParam = ParameterSpec.builder(namespaceIDClassName, "id").addAnnotation(NotNull.class).build();
            ParameterSpec keyIDParam = ParameterSpec.builder(keyIDClassName, "key").addAnnotation(NotNull.class).build();
            ParameterSpec stringIDParam = ParameterSpec.builder(ClassName.get(String.class), "id").addAnnotation(NotNull.class).build();


            // Getting
            {
                // code
                Class<? extends Annotation> annotation;
                if (defaultValue != null) {
                    annotation = NotNull.class;
                } else {
                    annotation = Nullable.class;
                }
                // javadoc
                StringBuilder javadoc = new StringBuilder("Returns the corresponding ");
                javadoc.append(typeName).append(" matching the given id. Returns ");
                if (defaultValue != null) {
                    javadoc.append('\'').append(defaultValue).append('\'');
                } else {
                    javadoc.append("null");
                }
                javadoc.append(" if none match.");

                // string variant
                MethodSpec idMethod = MethodSpec.methodBuilder("get" + typeName)
                        .returns(type)
                        .addAnnotation(annotation)
                        .addParameter(stringIDParam)
                        .addStatement("return $N.get($T.key($N))", registryField, ClassName.get("net.kyori.adventure.key", "Key"), stringIDParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addJavadoc(javadoc.toString())
                        .build();
                registriesClass.addMethod(idMethod);

                // NamespaceID variant
                registriesClass.addMethod(MethodSpec.methodBuilder("get" + typeName)
                        .returns(type)
                        .addAnnotation(annotation)
                        .addParameter(namespaceIDParam)
                        .addStatement("return $N.get($N)", registryField, namespaceIDParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addJavadoc(javadoc.toString())
                        .build());

                // Key variant
                registriesClass.addMethod(MethodSpec.methodBuilder("get" + typeName)
                        .returns(type)
                        .addAnnotation(annotation)
                        .addParameter(keyIDParam)
                        .addStatement("return $N.get($N)", registryField, keyIDParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addJavadoc(javadoc.toString().replace(" id.", " key."))
                        .build());
            }
            // Registration
            {
                ParameterSpec typeParam = ParameterSpec.builder(type, typeName.toLowerCase()).addAnnotation(NotNull.class).build();

                // javadoc

                // register method
                String javadoc = "Adds the given " + typeName + " to the registiry with the given id. " +
                        "Returns false if the " + typeName + " or the id is already registered.";
                registriesClass.addMethod(MethodSpec.methodBuilder("register" + typeName)
                        .returns(TypeName.BOOLEAN)
                        .addParameter(typeParam)
                        .addStatement("return $N.register($N)", registryField, typeParam)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addJavadoc(javadoc)
                        .build()
                );
            }

            // Is it an IDMapRegistry
            if (registryType.topLevelClassName().simpleName().contains("Id")) {
                // getNumericalId & fromNumericalId methods
                registriesClass.addMethod(
                        MethodSpec.methodBuilder("get" + typeName + "Id")
                                .returns(TypeName.INT)
                                .addParameter(ParameterSpec.builder(type, typeName.toLowerCase()).addAnnotation(NotNull.class).build())
                                .addStatement(
                                        "return $N.getId(" + typeName.toLowerCase() + ")",
                                        registryField
                                )
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .build()
                );
                registriesClass.addMethod(
                        MethodSpec.methodBuilder("get" + typeName)
                                .returns(type).addAnnotation(Nullable.class)
                                .addParameter(TypeName.INT, "id")
                                .addStatement(
                                        "return $N.get((short) id)",
                                        registryField
                                )
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .build()
                );
            }
            // Values method
            {
                registriesClass.addMethod(
                        MethodSpec.methodBuilder("get" + typeName + "s")
                                .returns(ParameterizedTypeName.get(ClassName.get(List.class), type))
                                .addAnnotation(Nullable.class)
                                .addStatement(
                                        "return $N.values()",
                                        registryField
                                )
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .build()
                );
            }
        }

        // Write files to outputFolder
        writeFiles(
                Collections.singletonList(
                        JavaFile.builder("net.minestom.server.registry", registriesClass.build()).build()
                ),
                outputFolder
        );
    }
}
