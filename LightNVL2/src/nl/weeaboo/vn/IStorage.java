package nl.weeaboo.vn;

import java.util.Collection;

/**
 * Provides general-purpose key/value storage. Storing null values isn't supported; any attempt to store a
 * null value will instead remove any existing value stored under that key.
 * <p>
 * Supported data types:
 * <ul>
 * <li>boolean</li>
 * <li>double</li>
 * <li>string</li>
 * </ul>
 * <p>
 * All numeric values are stored as double-precision floating point values internally.
 */
public interface IStorage {

    // === Functions ===========================================================
    /**
     * Removes all stored values.
     */
    public void clear();

    /**
     * Removes the value stored under the given key (if one exists).
     *
     * @return {@code true} if a successful.
     */
    public StoragePrimitive remove(String key);

    /**
     * Adds all mappings in {@code val} to this storage object, overwriting any existing values.
     *
     * @see #addAll(String, IStorage)
     */
    public void addAll(IStorage val);

    /**
     * Adds all mappings in {@code val} to this storage object, where all keys are prefixed with
     * {@code prefix}. Any existing values are overwritten.
     */
    public void addAll(String prefix, IStorage val);

    // === Getters =============================================================

    /**
     * @see #getKeys(String)
     */
    public Collection<String> getKeys();

    /**
     * @return A collection of all stored keys matching the given prefix, or all stored keys if the prefix is
     *         {@code null}.
     */
    public Collection<String> getKeys(String prefix);

    /**
     * @return {@code true} if this storage object contains a mapping for the given key.
     */
    public boolean contains(String key);

    /**
     * Returns raw value stored under the given key.
     */
    public StoragePrimitive get(String key);

    /**
     * Returns value stored under the given key converted to a boolean.
     *
     * @param defaultValue This value is returned instead if no value was stored under the given key, or if
     *        the value couldn't be converted to the desired return type.
     * @see #getString(String, String)
     */
    public boolean getBoolean(String key, boolean defaultValue);

    /**
     * Convenience method for retrieving a 32-bit signed integer. This is equivalent to calling
     * {@link IStorage#getDouble(String, double)} and casting the result to int.
     *
     * @see #getDouble(String, double)
     */
    public int getInt(String key, int defaultValue);

    /**
     * Convenience method for retrieving a floating-point value in single precision. This is equivalent to
     * calling {@link IStorage#getDouble(String, double)} and casting the result to float.
     *
     * @see #getDouble(String, double)
     */
    public float getFloat(String key, float defaultValue);

    /**
     * Returns value stored under the given key converted to a double.
     *
     * @param defaultValue This value is returned instead if no value was stored under the given key, or if
     *        the value couldn't be converted to the desired return type.
     * @see #getString(String, String)
     */
    public double getDouble(String key, double defaultValue);

    /**
     * Returns value stored under the given key converted to a string.
     *
     * @param defaultValue This value is returned instead if no value was stored under the given key.
     */
    public String getString(String key, String defaultValue);

    // === Setters =============================================================

    /**
     * Stores a value under the given key. If {@code null}, removes any existing mapping for the given key
     * instead.
     */
    public void set(String key, StoragePrimitive val);

    /**
     * Stores a boolean value under the given key.
     * @see #setString(String, String)
     */
    public void setBoolean(String key, boolean val);

    /**
     * Convenience method for storing an integer. All values are stored as double-precision floating point
     * internally.
     *
     * @see #setDouble(String, double)
     */
    public void setInt(String key, int val);

    /**
     * Convenience method for storing a single-precision floating point number. All values are stored as
     * double-precision floating point internally.
     *
     * @see #setDouble(String, double)
     */
    public void setFloat(String key, float val);

    /**
     * Stores a double-precision floating point value under the given key.
     *
     * @see #setString(String, String)
     */
    public void setDouble(String key, double val);

    /**
     * Stores a string value under the given key.
     *
     * @param val The value to store. If {@code null}, removes any existing mapping for the given key instead.
     */
    public void setString(String key, String val);

}
