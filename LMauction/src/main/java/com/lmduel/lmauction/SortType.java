package com.lmduel.lmauction;

public enum SortType {

    NEWEST("сначала новые"),
    OLDEST("сначала старые"),
    UNIT_CHEAP("дешевле за штуку"),
    UNIT_EXPENSIVE("дороже за штуку"),
    CHEAP("дешевле за лот"),
    EXPENSIVE("дороже за лот");

    private final String display;

    SortType(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    public SortType next() {
        SortType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public SortType prev() {
        SortType[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }
}