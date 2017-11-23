package me.saket.dank.ui.giphy;

import android.support.annotation.CheckResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.di.DankApi;
import me.saket.dank.ui.media.MediaHostRepository;
import timber.log.Timber;

/**
 * TODO: Ideally all of this should be accessed through {@link MediaHostRepository}.
 */
@Singleton
public class GiphyRepository {

  private static final int GIFS_TO_LOAD_PER_NETWORK_CALL = 30;

  private final DankApi dankApi;
  private Map<String, List<GiphyGif>> cache = new HashMap<>();

//  private List<GiphyGif> trendingGifs = new ArrayList<>();
//  private Map<String, List<GiphyGif>> searchGifs = new HashMap<>();

//  private BehaviorRelay<List<GiphyGif>> trendingStream = BehaviorRelay.create();
//  private BehaviorRelay<Map<String, List<GiphyGif>>> searchStream = BehaviorRelay.create();

  @Inject
  public GiphyRepository(DankApi dankApi) {
    this.dankApi = dankApi;
  }

  public void clear() {
    cache.clear();
  }

//  @CheckResult
//  public Observable<List<GiphyGif>> streamTrendingGifs() {
//    return trendingStream;
//  }

//  @CheckResult
//  private Observable<NetworkCallStatus> loadAndSaveMoreTrendingGifs() {
//    Relay<NetworkCallStatus> networkCallStatusStream = BehaviorRelay.create();
//    networkCallStatusStream.accept(NetworkCallStatus.createInFlight());
//
//    CompletionHandler<ListMediaResponse> listener = (ListMediaResponse listMediaResponse, Throwable throwable) -> {
//      if (throwable != null) {
//        networkCallStatusStream.accept(NetworkCallStatus.createFailed(throwable));
//        return;
//      }
//
//      List<GiphyGif> fetchedGifs = parseGiphyGifs(listMediaResponse);
//      trendingGifs.addAll(Collections.unmodifiableList(fetchedGifs));
//      trendingStream.accept(trendingGifs);
//      networkCallStatusStream.accept(NetworkCallStatus.createIdle());
//      //Timber.i("Fetched %s items", fetchedGifs.size());
//
//      // TODO: Kill stream once we reach the end.
////      Pagination paginationInfo = listMediaResponse.getPagination();
////      Timber.i("Pagination -> offset: %s, total count: %s", paginationInfo.getOffset(), paginationInfo.getTotalCount());
//    };
//
//    giphy.trending(MediaType.gif, GIFS_TO_LOAD_PER_NETWORK_CALL, trendingGifs.size(), null, listener);
//
//    dankApi.giphySearch()
//
//    return networkCallStatusStream;
//  }

//  @CheckResult
//  public Observable<List<GiphyGif>> streamSearchResults(String searchQuery) {
//    if (!searchGifs.containsKey(searchQuery)) {
//      searchGifs.put(searchQuery, Collections.emptyList());
//    }
//
//    return searchStream
//        .map(map -> map.get(searchQuery))
//        .startWith(searchGifs.get(searchQuery));
//  }

  @CheckResult
  public Single<List<GiphyGif>> search(String searchQuery) {
    if (cache.containsKey(searchQuery)) {
      return Single.just(cache.get(searchQuery));
    }

    Single<GiphySearchResponse> networkStream = searchQuery.isEmpty()
        ? dankApi.giphyTrending(DankApi.GIPHY_API_KEY, GIFS_TO_LOAD_PER_NETWORK_CALL, 0)
        : dankApi.giphySearch(DankApi.GIPHY_API_KEY, searchQuery, GIFS_TO_LOAD_PER_NETWORK_CALL, 0);

    return networkStream
        .map(response -> {
          List<GiphyGif> fetchedGifs = parseGiphyGifs(response);
          Timber.i("Fetched %s gifs for %s", fetchedGifs.size(), searchQuery);
          return fetchedGifs;
        })
        .doOnSuccess(giphyGifs -> cache.put(searchQuery, giphyGifs));
  }
//
//  @CheckResult
//  public Observable<NetworkCallStatus> searchAndSaveMoreGifs(String searchQuery) {
//    // Separate stream for notifying status updates so that the caller can stay synchronous.
//    int offset = searchGifs.get(searchQuery).size();
//    Timber.i("Searching: %s (offset: %s)", searchQuery, offset);
//
//    return dankApi.giphySearch(DankApi.GIPHY_API_KEY, searchQuery, GIFS_TO_LOAD_PER_NETWORK_CALL, offset)
//        .flatMap(response -> Single.fromCallable(() -> {
//          List<GiphyGif> fetchedGifs = parseGiphyGifs(response);
//          Timber.i("Fetched %s gifs for %s", fetchedGifs.size(), searchQuery);
//
//          List<GiphyGif> existingAndNewGifs = new ArrayList<>();
//          existingAndNewGifs.addAll(searchGifs.get(searchQuery));
//          existingAndNewGifs.addAll(Collections.unmodifiableList(fetchedGifs));
//          searchGifs.put(searchQuery, existingAndNewGifs);
//          searchStream.accept(searchGifs);
//
//          return NetworkCallStatus.createIdle();
//        }))
//        .onErrorReturn(throwable -> NetworkCallStatus.createFailed(throwable))
//        .toObservable()
//        .startWith(NetworkCallStatus.createInFlight());
//  }

  private List<GiphyGif> parseGiphyGifs(GiphySearchResponse response) {
    List<GiphySearchResponse.GiphyItem> giphyItems = response.items();
    List<GiphyGif> fetchedGifs = new ArrayList<>(giphyItems.size());

    for (GiphySearchResponse.GiphyItem giphyItem : giphyItems) {
      GiphySearchResponse.GifVariants gifVariants = giphyItem.gifVariants();
      String fullSizeUrl = gifVariants.downsizedUnder2mb().url();
      String thumbnailsPreviewUrl = gifVariants.fixedHeight200px().url();
      fetchedGifs.add(GiphyGif.create(giphyItem.id(), giphyItem.title(), giphyItem.url(), fullSizeUrl, thumbnailsPreviewUrl));
    }
    return fetchedGifs;
  }
}
