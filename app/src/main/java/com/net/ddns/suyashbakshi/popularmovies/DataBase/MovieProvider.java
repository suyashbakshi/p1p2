package com.net.ddns.suyashbakshi.popularmovies.DataBase;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Suyash on 2/27/2016.
 */
public class MovieProvider extends ContentProvider {

    public static final int MOVIE = 100;
    public static final int MOVIE_WITH_SORT = 101;
    public static final int MOVIE_WITH_SORT_AND_ID = 102;
    public static final int SORT = 200;
    public static final int FAV = 300;
    public static final int FAV_WITH_ID = 301;

    private static final UriMatcher mUriMatcher = buildUriMatcher();
    private MovieDbHelper mDbHelper;

    private static final SQLiteQueryBuilder mMovieBySortQueryBuilder;
    private static final SQLiteQueryBuilder mFavByIdQueryBuilder;

    static {
        mMovieBySortQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //movies INNER JOIN sort ON movies.sort_id = sort._id
        mMovieBySortQueryBuilder.setTables(MoviesContract.MoviesEntry.TABLE_NAME + " INNER JOIN " +
                MoviesContract.SortEntry.TABLE_NAME + " ON " +
                MoviesContract.MoviesEntry.COLUMN_SORT_KEY + " = " +
                MoviesContract.SortEntry.TABLE_NAME + "." +
                MoviesContract.SortEntry._ID);
    }

    static {
        mFavByIdQueryBuilder = new SQLiteQueryBuilder();

        mFavByIdQueryBuilder.setTables(MoviesContract.FavoriteEntry.TABLE_NAME);
    }

    //sort.sort_value = ?                           "This is the selection statement for main view QUERY"
    private static final String sSortValueSelection = MoviesContract.SortEntry.TABLE_NAME + "." + MoviesContract.SortEntry.COLUMN_SORT_VALUE + " = ?";

    //sort.sort_value = ? AND movie_id = ?         "This is the selection statement for details view QUERY"
    private static final String sSortValueWithIdSelection = MoviesContract.SortEntry.COLUMN_SORT_VALUE + " = ? AND " + MoviesContract.MoviesEntry.COLUMN_MOVIE_ID + " = ?";

    //for favorite table : movie_id = ?             "This is the selection for details view for FAVORITE TABLE
    private static final String sFavValueWithIdSelection = MoviesContract.FavoriteEntry.COLUMN_MOVIE_ID + " = ?";

    //    this function returns cursor for favorite table containing movie with specific Movie_ID;
    private Cursor getFavById(Uri uri, String[] projection, String sortOrder) {

        long movieId = MoviesContract.FavoriteEntry.getMovieIdFromUri(uri);

        String[] selectionArgs = {Long.toString(movieId)};

        return mFavByIdQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                sFavValueWithIdSelection,
                selectionArgs,
                null, null,
                sortOrder);
    }

    //    this function returns the cursor that contains data of MOVIES for the main view based upon sort value.
    private Cursor getMovieBySortValue(Uri uri, String[] projection, String sortOrder) {

        String sortValue = MoviesContract.MoviesEntry.getSortFromUri(uri);

        String[] selectionArgs;

        selectionArgs = new String[]{sortValue};

        return mMovieBySortQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                sSortValueSelection,
                selectionArgs,
                null, null,
                sortOrder);
    }

    //    this function returns the cursor that helps to get the movie details for the detail view based upon sort value and movie ID
    private Cursor getMovieBySortValueAndId(Uri uri, String[] projection, String sortOrder) {

        String sortValue = MoviesContract.MoviesEntry.getSortFromUri(uri);
        long movieId = MoviesContract.MoviesEntry.getIdFromUri(uri);

        return mMovieBySortQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                sSortValueWithIdSelection,
                new String[]{sortValue, Long.toString(movieId)},
                null, null,
                sortOrder);
    }


    @Override
    public boolean onCreate() {
        mDbHelper = new MovieDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        Cursor retCursor;

        switch (mUriMatcher.match(uri)) {

            case MOVIE_WITH_SORT:
                retCursor = getMovieBySortValue(uri, projection, sortOrder);
                break;

            case MOVIE_WITH_SORT_AND_ID:
                Log.v("URI_ON_CLICK", String.valueOf(uri));
                retCursor = getMovieBySortValueAndId(uri, projection, sortOrder);
                break;

            case FAV_WITH_ID:
                retCursor = getFavById(uri, projection, sortOrder);
                break;

            case MOVIE:
                retCursor = mDbHelper.getReadableDatabase().query(MoviesContract.MoviesEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, null,
                        sortOrder);
                break;

            case SORT:
                retCursor = mDbHelper.getReadableDatabase().query(MoviesContract.SortEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, null,
                        sortOrder);
                break;

            case FAV:
                retCursor = mDbHelper.getReadableDatabase().query(MoviesContract.FavoriteEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, null,
                        sortOrder);
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }


        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {

        final int match = mUriMatcher.match(uri);

        switch (match) {
            case MOVIE:
                return MoviesContract.MoviesEntry.CONTENT_TYPE;
            case SORT:
                return MoviesContract.SortEntry.CONTENT_TYPE;
            case FAV:
                return MoviesContract.FavoriteEntry.CONTENT_TYPE;
            case MOVIE_WITH_SORT:
                return MoviesContract.MoviesEntry.CONTENT_TYPE;
            case MOVIE_WITH_SORT_AND_ID:
                return MoviesContract.MoviesEntry.CONTENT_ITEM_TYPE;
            case FAV_WITH_ID:
                return MoviesContract.FavoriteEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = mUriMatcher.match(uri);
        Uri retUri;

        switch (match) {
            case MOVIE: {
                long _id = db.insert(MoviesContract.MoviesEntry.TABLE_NAME, null, values);

                if (_id > 0) {
                    retUri = MoviesContract.MoviesEntry.buildMoviesUri(_id);
                    Log.v("ROW_INSERTED", String.valueOf(retUri));
                } else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;

            }
            case SORT: {
                long _id = db.insert(MoviesContract.SortEntry.TABLE_NAME, null, values);

                if (_id > 0) {
                    retUri = MoviesContract.SortEntry.buildSortUri(_id);
                    Log.v("ROW_INSERTED", String.valueOf(retUri));
                } else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case FAV: {
                long _id = db.insert(MoviesContract.FavoriteEntry.TABLE_NAME, null, values);

                if (_id > 0) {
                    retUri = MoviesContract.FavoriteEntry.buildFavUri(_id);
                    Log.v("ROW_INSERTED", String.valueOf(retUri));
                } else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown Uri " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        db.close();
        return retUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = mUriMatcher.match(uri);
        final int rowsDeleted;

        switch (match) {
            case MOVIE:
                rowsDeleted = db.delete(MoviesContract.MoviesEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case SORT:
                rowsDeleted = db.delete(MoviesContract.SortEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case FAV:
                rowsDeleted = db.delete(MoviesContract.FavoriteEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri " + uri);
        }

        if (rowsDeleted != 0)
            getContext().getContentResolver().notifyChange(uri, null);


        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = mUriMatcher.match(uri);
        final int rowsUpdated;

        switch (match) {

            case MOVIE: {
                rowsUpdated = db.update(MoviesContract.MoviesEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case SORT: {
                rowsUpdated = db.update(MoviesContract.SortEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case FAV:
                rowsUpdated = db.update(MoviesContract.FavoriteEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri " + uri);
        }
        if (rowsUpdated != 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = mUriMatcher.match(uri);
        switch (match) {
            case MOVIE:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(MoviesContract.MoviesEntry.TABLE_NAME, null, value);
                        Uri muri = MoviesContract.MoviesEntry.buildMoviesUri(_id);
                        Log.v("MOVIE_URI", String.valueOf(muri));
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    public static UriMatcher buildUriMatcher() {
        final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MoviesContract.CONTENT_AUTHORITY;

        mUriMatcher.addURI(authority, MoviesContract.PATH_MOVIES, MOVIE);
        mUriMatcher.addURI(authority, MoviesContract.PATH_MOVIES + "/*", MOVIE_WITH_SORT);
        mUriMatcher.addURI(authority, MoviesContract.PATH_MOVIES + "/*/#", MOVIE_WITH_SORT_AND_ID);
        mUriMatcher.addURI(authority, MoviesContract.PATH_SORT, SORT);
        mUriMatcher.addURI(authority, MoviesContract.PATH_FAV, FAV);
        mUriMatcher.addURI(authority, MoviesContract.PATH_FAV + "/#", FAV_WITH_ID);

        return mUriMatcher;
    }
}
