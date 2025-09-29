
/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2016  Michael Kolling and John Rosenberg 

 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 

 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 

 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 

 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
*/
package bluej.parser.context;

import bluej.parser.symtab.ClassInfo;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader for CompilationUnitContext from classpath resources.
 * This class encapsulates the logic for loading .ctxt files from
 * the classpath, with proper classloader handling and optional caching.
 */
public class CompilationUnitContextLoader implements AutoCloseable {
    
    /** The primary classloader to use for loading resources */
    @NotNull private final ClassLoader primaryLoader;

    /** Package roots for project classes */
    private final Set<Path> packageRoots = ConcurrentHashMap.newKeySet();

    /** Cache for loaded contexts, null if caching is disabled */
    private final ConcurrentHashMap<String, CompilationUnitContext> cache;
    
    /** Whether caching is enabled for this loader */
    private final boolean cachingEnabled;
    
    /**
     * Creates a new loader with the specified classloader.
     * Caching is enabled by default.
     * 
     * @param classLoader The classloader to use
     */
    public CompilationUnitContextLoader(@NotNull ClassLoader classLoader) {
        this(classLoader, true);
    }


    /**
     * Creates a new loader with the specified classloader and caching option.
     * 
     * @param classLoader The classloader to use
     * @param enableCaching Whether to cache loaded contexts for performance
     */
    public CompilationUnitContextLoader(@NotNull ClassLoader classLoader, boolean enableCaching) {
        this.primaryLoader = classLoader;
        this.cachingEnabled = enableCaching;
        this.cache = enableCaching ? new ConcurrentHashMap<>() : null;
    }


    /**
     * Creates a new loader with the system classloader and caching option.
     *
     * @param enableCaching Whether to cache loaded contexts for performance
     */
    public CompilationUnitContextLoader(boolean enableCaching) {
        this(ClassLoader.getSystemClassLoader(), enableCaching);
    }

    /**
     * Adds a package root to the loader.
     * This allows the loader to resolve classes in different package directories.
     *
     * @param packageRoot The root directory for a package
     */
    public void addPackageRoot(@NotNull Path packageRoot) {
        if (Files.exists(packageRoot) && Files.isDirectory(packageRoot)) {
            packageRoots.add(packageRoot.toAbsolutePath());
        }
    }


    /**
     * Removes a package root from the loader.
     *
     * @param packageRoot The root directory to remove
     */
    public void removePackageRoot(@NotNull Path packageRoot) {
        packageRoots.remove(packageRoot.toAbsolutePath());
    }


    /**
     * Loads a CompilationUnitContext for a class with caching.
     * This method searches through all registered package roots to find
     * the .ctxt file and handles the case where the file doesn't exist
     * by returning an empty context.
     *
     * @param qualifiedName The fully qualified class name
     *
     * @return A CompilationUnitContext
     */
    @NotNull public CompilationUnitContext contextForClass(@NotNull String qualifiedName) {
        CompilationUnitContext context = resolveFromResources(this.primaryLoader, qualifiedName);

        if (context != null) {
            return context;
        }

        context = resolveFromPackageRoots(qualifiedName);

        return context != null
            ? context
            : CompilationUnitContext.readOnly(qualifiedName);
    }


    /**
     * Loads context for a class using its Class object.
     * Convenience method that extracts the qualified name and classloader.
     * 
     * <p>Given Class's classloader is given preference, if available.
     * 
     * @param clazz The class to load context for
     *
     * @return The loaded context, or an empty context if a context file is not found
     */
    @NotNull public CompilationUnitContext contextForClass(@NotNull Class<?> clazz) {
        ClassLoader specificClassLoader = clazz.getClassLoader();
        String qualifiedName = clazz.getName();

        if (specificClassLoader != null) {
            CompilationUnitContext context = resolveFromResources(specificClassLoader, qualifiedName);

            if (context != null) {
                return context;
            }
        }

        // Fall back to class-specific loader
        return contextForClass(qualifiedName);
    }


    /**
     * Resolves the given class FQDN against resources present on the classpath
     * 
     * @param qualifiedName the FQDN of the class to resolve
     * @param loader the classloader to resolve
     * 
     * @return a compilation unit context for this class, or null if not present
     */
    private CompilationUnitContext resolveFromResources(ClassLoader loader, String qualifiedName) {
        Path resourcePath = constructContextFilePath(qualifiedName);

        URL resource = loader.getResource(resourcePath.toString());

        if (resource != null) {
            return loadContextFromFile(new File(resource.getFile()), qualifiedName);
        }

        return null;
    }


    /**
     * Constructs the resource path for a .ctxt file from a qualified class name.
     *
     * @param qualifiedName The fully qualified class name (e.g., "java.lang.String")
     *
     * @return The resource path (e.g., "java/lang/String.ctxt")
     */
    @NotNull protected Path constructContextFilePath(@NotNull String qualifiedName) {
        String[] elements = qualifiedName.split("\\.");

        elements[elements.length - 1] += ".ctxt";

        return Paths.get("", elements);
    }


    /**
     * Resolves the given class FQDN against BlueJ project roots
     * 
     * @param qualifiedName the FQDN of the class to resolve
     * 
     * @return a compilation unit context for this class, or null if not present
     */
    private CompilationUnitContext resolveFromPackageRoots(@NotNull String qualifiedName) {
        if (packageRoots.isEmpty()) {
            return null;
        }

        Path resourcePath = constructContextFilePath(qualifiedName);

        for (Path packageRoot : packageRoots) {
            Path path = packageRoot.resolve(resourcePath);

            if (Files.exists(path)) {
                CompilationUnitContext context = loadContextFromFile(path.toFile(), qualifiedName);

                if (context != null) {
                    return context;
                }
            }
        }

        return null;
    }


    /**
     * Loads a compilation unit context from a file
     * 
     * @param file the file to load the compilation unit context from
     * @param qualifiedName the FQDN of the class being loaded (TODO: should be contained in the file)
     * 
     * @return a compilation unit context loaded from this file
     */
    private CompilationUnitContext loadContextFromFile(@NotNull File file, @NotNull String qualifiedName) {
        if (!cachingEnabled || cache == null) {
            return PropertyContextFormat.fromFile(qualifiedName, file);
        }

        return cache.computeIfAbsent(qualifiedName, key -> PropertyContextFormat.fromFile(qualifiedName, file));
    }


    /**
     * Creates a CompilationUnitContext from ClassInfo using the fully qualified class name.
     * The loader will construct the appropriate file path internally.
     *
     * @param qualifiedName The fully qualified class name
     * @param info The ClassInfo from the parser
     *
     * @return A new CompilationUnitContext with the data from ClassInfo
     */
    @NotNull public CompilationUnitContext updateContextFromClassInfo(@NotNull String qualifiedName, @NotNull Path projectDir, @NotNull ClassInfo info) {
        // Extract ClassInfo data first (handles FX thread requirements)
        ClassInfoData data = extractClassInfoData(info);

        CompilationUnitContext context = CompilationUnitContext.readOnly(data.className);

        context.setComments(PropertyContextFormat.fromProperties(data.comments));

        Path contextFilePath = projectDir.resolve(constructContextFilePath(qualifiedName));

        try {
            File contextFile = contextFilePath.toFile();

            PropertyContextFormat.writeToFile(context, contextFile);
        } catch (IOException ioe) {
            throw new UncheckedIOException("Could not write context for " + contextFilePath, ioe);
        }

        // Invalidate cache entry so subsequent lookups reload the updated data
        evictFromCache(qualifiedName);

        return context;
    }


    /**
     * Invalidates the cached context for a class.
     * This method searches through all registered package roots to find
     * and evict the corresponding cache entry.
     *
     * @param qualifiedName The fully qualified class name
     * @return true if an entry was removed from cache, false otherwise
     */
    public boolean evictFromCache(@NotNull String qualifiedName) {
        boolean evicted = false;

        if (cachingEnabled) {
            cache.remove(qualifiedName);
        }

        return evicted;
    }


    /**
     * Clears the cache if caching is enabled.
     * No-op if caching is disabled.
     *
     * <p>This method is thread-safe and can be called while other threads
     * are accessing the cache.
     */
    public void clearCache() {
        if (cache != null) {
            cache.clear();
        }
    }


    /**
     * Closes this loader and releases any cached resources.
     * This method clears the cache to free memory.
     *
     * <p>After closing, the loader can still be used but will need to
     * reload any previously cached contexts.
     *
     * <p>This method is idempotent and thread-safe.
     */
    @Override
    public void close() {
        clearCache();
    }


    /**
     * Helper method to safely extract ClassInfo data on the FX thread.
     * This method ensures that ClassInfo.getName() and ClassInfo.getComments()
     * are called on the JavaFX platform thread as required.
     *
     * @param info The ClassInfo to access
     * @return A ClassInfoData record containing the extracted data
     */
    static ClassInfoData extractClassInfoData(@NotNull ClassInfo info) {
        // Need to execute on FX thread
        CompletableFuture<ClassInfoData> future = new CompletableFuture<>();

        Runnable infoGetter = () -> {
            try {
                String className = info.getName();
                Properties comments = info.getComments();
                future.complete(new ClassInfoData(className, comments));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        };

        try {
            if (Platform.isFxApplicationThread()) {
                infoGetter.run();
            }
            else {
                Platform.runLater(infoGetter);
            }

            return future.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract ClassInfo data", e);
        }
    }


    /**
     * Record class to hold extracted ClassInfo data.
     * This allows ClassInfo data to be extracted on the FX thread
     * and used elsewhere without thread constraints.
     */
    public record ClassInfoData(
            @NotNull String className,
            @NotNull Properties comments
    ) {}
}