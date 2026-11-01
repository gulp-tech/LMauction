package com.lmduel.lmauction;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.UUID;

public class AHHolder implements InventoryHolder {

    private Inventory inventory;
    private int page;
    private AuctionCategory category;
    private View view;
    private SortType sortType;
    private UUID filterSeller;
    private String filterSellerName;
    private String searchQuery;

    public enum View {
        LOTS,
        CATEGORIES
    }

    public AHHolder(int page, AuctionCategory category, View view, SortType sortType, UUID filterSeller, String filterSellerName, String searchQuery) {
        this.page = page;
        this.category = (category != null) ? category : AuctionCategory.ALL;
        this.view = (view != null) ? view : View.LOTS;
        this.sortType = (sortType != null) ? sortType : SortType.NEWEST;
        this.filterSeller = filterSeller;
        this.filterSellerName = filterSellerName;
        this.searchQuery = searchQuery;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public AuctionCategory getCategory() { return category; }
    public void setCategory(AuctionCategory category) { this.category = category; }

    public View getView() { return view; }
    public void setView(View view) { this.view = view; }

    public SortType getSortType() { return sortType; }
    public void setSortType(SortType sortType) { this.sortType = sortType; }

    public UUID getFilterSeller() { return filterSeller; }
    public void setFilterSeller(UUID filterSeller) { this.filterSeller = filterSeller; }

    public String getFilterSellerName() { return filterSellerName; }
    public void setFilterSellerName(String filterSellerName) { this.filterSellerName = filterSellerName; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
}