package com.github.TavstalDev.rebus.services.nbt;

import com.github.TavstalDev.yggraLib.bukkit.items.INbtAccessor;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTType;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class NbtAccessor implements INbtAccessor {
    @Override
    public @Nullable String getString(ItemStack itemStack, String s) {
        return NBT.get(itemStack, nbt ->
                (String)nbt.getString(s)
        );
    }

    @Override
    public @Nullable Integer getInteger(ItemStack itemStack, String s) {
        return NBT.get(itemStack, nbt ->
                (Integer) nbt.getInteger(s)
        );
    }

    @Override
    public @Nullable Double getDouble(ItemStack itemStack, String s) {
        return NBT.get(itemStack, nbt ->
                (Double) nbt.getDouble(s)
        );
    }

    @Override
    public @Nullable Float getFloat(ItemStack itemStack, String s) {
        return NBT.get(itemStack, nbt ->
                (Float)nbt.getFloat(s)
        );
    }

    @Override
    public @Nullable Long getLong(ItemStack itemStack, String s) {
        return NBT.get(itemStack, nbt ->
                (Long)nbt.getLong(s)
        );
    }

    @Override
    public @Nullable Byte getByte(ItemStack itemStack, String s) {
        return NBT.get(itemStack, nbt ->
                (Byte) nbt.getByte(s)
        );
    }

    @Override
    public @Nullable Short getShort(ItemStack itemStack, String s) {
        return NBT.get(itemStack, nbt ->
                (Short) nbt.getShort(s)
        );
    }

    @Override
    public @Nullable Boolean getBoolean(ItemStack itemStack, String s) {
        return NBT.get(itemStack, nbt ->
                (Boolean) nbt.getBoolean(s)
        );
    }

    @Override
    public int[] getIntegerArray(ItemStack itemStack, String s) {
        return NBT.get(itemStack, nbt ->
                (int[])nbt.getIntArray(s)
        );
    }


    @Override
    public long[] getLongArray(ItemStack itemStack, String s) {
        return NBT.get(itemStack, nbt ->
                (long[])nbt.getLongArray(s)
        );
    }

    @Override
    public byte[] getByteArray(ItemStack itemStack, String s) {
        return NBT.get(itemStack, nbt ->
                (byte[])nbt.getByteArray(s)
        );
    }

    @Override
    public @Nullable <T> T getOrDefault(ItemStack itemStack, String s, @Nullable T t) {
        return NBT.get(itemStack, nbt ->
                (T)nbt.getOrDefault(s, t)
        );
    }

    @Override
    public Map<String, Object> getAll(ItemStack itemStack) {
        Map<String, Object> result = new HashMap<>();
        NBT.get(itemStack, nbt -> {
            for (String key : nbt.getKeys()) {
                Object value = extractValue(nbt, key);
                if (value != null) {
                    result.put(key, value);
                }
            }
        });
        return result;
    }

    @Override
    public void set(ItemStack itemStack, String s, @Nullable String s1) {
        NBT.modify(itemStack, nbt -> {
            nbt.setString(s, s1);
        });
    }

    @Override
    public void set(ItemStack itemStack, String s, @Nullable Integer integer) {
        NBT.modify(itemStack, nbt -> {
            nbt.setInteger(s, integer);
        });
    }

    @Override
    public void set(ItemStack itemStack, String s, @Nullable Double aDouble) {
        NBT.modify(itemStack, nbt -> {
            nbt.setDouble(s, aDouble);
        });
    }

    @Override
    public void set(ItemStack itemStack, String s, @Nullable Float aFloat) {
        NBT.modify(itemStack, nbt -> {
            nbt.setFloat(s, aFloat);
        });
    }

    @Override
    public void set(ItemStack itemStack, String s, @Nullable Long aLong) {
        NBT.modify(itemStack, nbt -> {
            nbt.setLong(s, aLong);
        });
    }

    @Override
    public void set(ItemStack itemStack, String s, @Nullable Short aShort) {
        NBT.modify(itemStack, nbt -> {
            nbt.setShort(s, aShort);
        });
    }

    @Override
    public void set(ItemStack itemStack, String s, @Nullable Boolean aBoolean) {
        NBT.modify(itemStack, nbt -> {
            nbt.setBoolean(s, aBoolean);
        });
    }

    @Override
    public void set(ItemStack itemStack, String s, int[] integers) {
        NBT.modify(itemStack, nbt -> {
            nbt.setIntArray(s, integers);
        });
    }

    @Override
    public void set(ItemStack itemStack, String s, long[] longs) {
        NBT.modify(itemStack, nbt -> {
            nbt.setLongArray(s, longs);
        });
    }

    @Override
    public void set(ItemStack itemStack, String s, byte[] bytes) {
        NBT.modify(itemStack, nbt -> {
            nbt.setByteArray(s, bytes);
        });
    }

    @Override
    public void remove(ItemStack itemStack, String key) {
        NBT.modify(itemStack, nbt -> {
            nbt.removeKey(key);
        });
    }

    private Object extractValue(ReadableNBT nbt, String key) {
        NBTType type = nbt.getType(key);
        if (type == null) {
            return null;
        }

        switch (type) {
            case NBTTagByte:
                return nbt.getByte(key);
            case NBTTagShort:
                return nbt.getShort(key);
            case NBTTagInt:
                return nbt.getInteger(key);
            case NBTTagLong:
                return nbt.getLong(key);
            case NBTTagFloat:
                return nbt.getFloat(key);
            case NBTTagDouble:
                return nbt.getDouble(key);
            case NBTTagByteArray:
                return nbt.getByteArray(key);
            case NBTTagIntArray:
                return nbt.getIntArray(key);
            case NBTTagCompound: {
                ReadableNBT compound = nbt.getCompound(key);
                if (compound == null) {
                    return null;
                }
                Map<String, Object> nested = new HashMap<>();
                for (String subKey : compound.getKeys()) {
                    nested.put(subKey, extractValue(compound, subKey));
                }
                return nested;
            }
            case NBTTagList: {
                NBTType listType = nbt.getListType(key);
                if (listType == NBTType.NBTTagString) {
                    return nbt.getStringList(key);
                } else if (listType == NBTType.NBTTagInt) {
                    return nbt.getIntegerList(key);
                } else if (listType == NBTType.NBTTagCompound) {
                    return nbt.getCompoundList(key);
                }
                return nbt.getStringList(key);
            }
            default:
                return nbt.getString(key);
        }
    }
}
