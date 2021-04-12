package com.minestom.code_generation.map;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.minestom.code_generation.MinestomCodeGenerator;
import com.squareup.javapoet.*;
import net.minestom.server.map.MapColor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class MapColorsGenerator extends MinestomCodeGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapColorsGenerator.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final File DEFAULT_INPUT_FILE = new File(DEFAULT_SOURCE_FOLDER_ROOT + "/json", "map_colors.json");
    private final File mapColorsFile;
    private final File outputFolder;

    public MapColorsGenerator() {
        this(null, null);
    }

    public MapColorsGenerator(@Nullable File mapColorsFile) {
        this(mapColorsFile, null);
    }

    public MapColorsGenerator(@Nullable File mapColorsFile, @Nullable File outputFolder) {
        this.mapColorsFile = Objects.requireNonNullElse(mapColorsFile, DEFAULT_INPUT_FILE);
        this.outputFolder = Objects.requireNonNullElse(outputFolder, DEFAULT_OUTPUT_FOLDER);
    }

    @Override
    public void generate() {
        if (!mapColorsFile.exists()) {
            LOGGER.error("Failed to find mapColors.json.");
            LOGGER.error("Stopped code generation for mapColors.");
            return;
        }
        if (!outputFolder.exists() && !outputFolder.mkdirs()) {
            LOGGER.error("Output folder for code generation does not exist and could not be created.");
            return;
        }

        JsonArray mapColors;
        try {
            mapColors = GSON.fromJson(new JsonReader(new FileReader(mapColorsFile)), JsonArray.class);
        } catch (FileNotFoundException e) {
            LOGGER.error("Failed to find mapColors.json.");
            LOGGER.error("Stopped code generation for mapColors.");
            return;
        }
        ClassName mapColorsClassName = ClassName.get("net.minestom.server.map", "MapColors");
        // Particle
        TypeSpec.Builder mapColorsClass = TypeSpec.classBuilder(mapColorsClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL).addJavadoc("AUTOGENERATED");
        mapColorsClass.addField(
                FieldSpec.builder(
                        ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(MapColor.class)),
                        "mapColors"
                )
                        .initializer("new $T<>()", ClassName.get(LinkedList.class))
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .build()
        );
        mapColorsClass.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build()
        );
        // add values() method
        mapColorsClass.addMethod(
                MethodSpec.methodBuilder("values")
                        .returns(ArrayTypeName.of(ClassName.get(MapColor.class)))
                        .addStatement("return mapColors.toArray(new MapColor[0])")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .build()
        );
        CodeBlock.Builder staticBlock = CodeBlock.builder();
        // Use data
        for (JsonElement mc : mapColors) {
            JsonObject mapColor = mc.getAsJsonObject();

            String mapColorName = mapColor.get("name").getAsString();
            int red = (mapColor.get("color").getAsInt() >> 16) & 0xff;
            int green = (mapColor.get("color").getAsInt() >> 8) & 0xff;
            int blue = mapColor.get("color").getAsInt() & 0xff;

            mapColorsClass.addField(
                    FieldSpec.builder(
                            ClassName.get(MapColor.class),
                            mapColorName
                    ).initializer(
                            "new $T($L, $L, $L, $L)",
                            ClassName.get(MapColor.class),
                            mapColor.get("id").getAsInt(),
                            red,
                            green,
                            blue
                    ).addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).build()
            );
            // Add to static init.
            staticBlock.addStatement("mapColors.add($N)", mapColorName);
        }

        mapColorsClass.addStaticBlock(staticBlock.build());

        // Write files to outputFolder
        writeFiles(
                Collections.singletonList(
                        JavaFile.builder("net.minestom.server.map", mapColorsClass.build()).build()
                ),
                outputFolder
        );
    }
}
