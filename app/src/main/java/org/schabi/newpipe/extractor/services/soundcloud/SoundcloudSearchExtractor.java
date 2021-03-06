package org.schabi.newpipe.extractor.services.soundcloud;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.Downloader;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.InfoItemExtractor;
import org.schabi.newpipe.extractor.InfoItemsCollector;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.InfoItemsSearchCollector;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.utils.Parser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Nonnull;

import static org.schabi.newpipe.extractor.services.soundcloud.SoundcloudSearchQueryHandlerFactory.ITEMS_PER_PAGE;

public class SoundcloudSearchExtractor extends SearchExtractor {

    private JsonArray searchCollection;

    public SoundcloudSearchExtractor(StreamingService service,
                                     SearchQueryHandler urlIdHandler,
                                     String contentCountry) {
        super(service, urlIdHandler, contentCountry);
    }

    @Override
    public String getSearchSuggestion() throws ParsingException {
        return null;
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        return new InfoItemsPage<>(collectItems(searchCollection), getNextPageUrl());
    }

    @Override
    public String getNextPageUrl() throws IOException, ExtractionException {
        return getNextPageUrlFromCurrentUrl(getUrl());
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(String pageUrl) throws IOException, ExtractionException {
        final Downloader dl = getDownloader();
        System.err.println("line  no 58 getPage :SoundcloudSearchExtractor.java");

        try {
            searchCollection = JsonParser.object().from(dl.download(pageUrl)).getArray("collection");
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }

        return new InfoItemsPage<>(collectItems(searchCollection), getNextPageUrlFromCurrentUrl(pageUrl));
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        final Downloader dl = getDownloader();
        final String url = getUrl();

        System.err.println("line  no 74 onFetchPage :SoundcloudSearchExtractor.java");

        try {
            searchCollection = JsonParser.object().from(dl.download(url)).getArray("collection");
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }

        if (searchCollection.size() == 0) {
            throw new NothingFoundException("Nothing found");
        }
    }

    private InfoItemsCollector<InfoItem, InfoItemExtractor> collectItems(JsonArray searchCollection) {
        final InfoItemsSearchCollector collector = getInfoItemSearchCollector();

        for (Object result : searchCollection) {
            if (!(result instanceof JsonObject)) continue;
            //noinspection ConstantConditions
            JsonObject searchResult = (JsonObject) result;
            String kind = searchResult.getString("kind", "");
            switch (kind) {
                case "user":
                    collector.commit(new SoundcloudChannelInfoItemExtractor(searchResult));
                    break;
                case "track":
                    collector.commit(new SoundcloudStreamInfoItemExtractor(searchResult));
                    break;
                case "playlist":
                    collector.commit(new SoundcloudPlaylistInfoItemExtractor(searchResult));
                    break;
            }
        }

        return collector;
    }

    private String getNextPageUrlFromCurrentUrl(String currentUrl)
            throws MalformedURLException, UnsupportedEncodingException {
        final int pageOffset = Integer.parseInt(
                Parser.compatParseMap(
                        new URL(currentUrl)
                                .getQuery())
                        .get("offset"));

        return currentUrl.replace("&offset=" +
                        Integer.toString(pageOffset),
                "&offset=" + Integer.toString(pageOffset + ITEMS_PER_PAGE));
    }
}
