package com.github.oogasawa.utility.sau3.opensearch;

public class SitemapEntry {
    String url = null;
    String lastmod = null;;


    public SitemapEntry() {  }
    
    public SitemapEntry(String url, String lastmod) {
        this.url = url;
        this.lastmod = lastmod;
    }


    // --------------------
    // Getter and Setter
    // --------------------

    
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLastmod() {
        return lastmod;
    }

    public void setLastmod(String lastmod) {
        this.lastmod = lastmod;
    }

    
    
}
