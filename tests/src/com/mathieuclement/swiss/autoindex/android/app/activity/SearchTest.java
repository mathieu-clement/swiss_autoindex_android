package com.mathieuclement.swiss.autoindex.android.app.activity;

import android.test.ActivityInstrumentationTestCase2;
import com.jayway.android.robotium.solo.Solo;
import com.mathieuclement.swiss.autoindex.android.app.fragments.search.LastSearchesProvider;
import junit.framework.Assert;

public class SearchTest extends
        ActivityInstrumentationTestCase2<WelcomeListActivity> {

    private final String SEARCH_TEXT = "Search";
    private final String ADD_BOOKMARK_TEXT = "Bookmark";
    private final String REMOVE_BOOKMARK_TEXT = "Remove bookmark";
    private Solo solo;

    public SearchTest() {
        super(WelcomeListActivity.class);
    }

    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
        LastSearchesProvider.getInstance().clear();
    }

    public void testFribourgSearchAndBookmarks() throws Exception {
        // Start search activity
        solo.clickOnText(SEARCH_TEXT);

        // Set search to 300 310

        solo.enterText(0, "300310");
        // Assume AG was selected
        //solo.pressSpinnerItem(0, +5); // Select "FR"
        solo.clickOnButton(1);

        Assert.assertTrue(solo.waitForActivity("ShowPlateOwnerActivity", 20000)); // 20 seconds timeout

        String expectedPlateText = "FR · 300 310";
        Assert.assertEquals("Plate canton and number found", expectedPlateText, solo.getText(1).getText());
        String expectedTpfText = "Transports Publics Fribourgeois\r\nRue Louis-d'Affry 2\r\nCase postale 1536\r\n1700 Fribourg";
        Assert.assertEquals("Owner found", expectedTpfText, solo.getText(2).getText().toString());

        // Test favourite button has the text as if the plate is not a favourite
        String addToFavoritesText = ADD_BOOKMARK_TEXT;
        Assert.assertEquals(addToFavoritesText, solo.getButton(1).getText().toString());

        // Add to favourites
        solo.clickOnText(addToFavoritesText);
        // Check added
        String favoriteTpfText = "FR 300 310 - Transports Publics Fribourgeois";
        Assert.assertTrue("Favorite added to list", solo.searchText(favoriteTpfText));
        // Click on it
        solo.clickOnText(favoriteTpfText);
        // Check view contains the same text
        Assert.assertTrue(solo.waitForActivity("ShowPlateOwnerActivity", 20000)); // 20 seconds timeout
        Assert.assertEquals(expectedPlateText, solo.getText(1).getText().toString());
        Assert.assertEquals(expectedTpfText, solo.getText(2).getText().toString());
        // Check second button is now "Supprimer des favoris"
        String removeFromFavoritesText = REMOVE_BOOKMARK_TEXT;
        Assert.assertEquals(removeFromFavoritesText, solo.getButton(1).getText().toString());
        // Click on it
        solo.clickOnText(removeFromFavoritesText);

        // Go back to bookmarks
        solo.goBack();
        // Check favorite is removed
        Assert.assertFalse(solo.searchText(favoriteTpfText));
    }

    /*public void testValaisSearch() throws Exception {
        // Start search activity
        solo.clickOnText(SEARCH_TEXT);

        // Set search to 300 310

        solo.enterText(0, "50000");
        // Assume FR was selected
        solo.pressSpinnerItem(0, +17); // Select "VS"
        solo.clickOnButton(1);

        Assert.assertTrue(solo.waitForActivity("ShowPlateOwnerActivity", 60000)); // 1 minute timeout

        String expectedPlateText = "VS · 50 000";
        Assert.assertEquals(expectedPlateText, solo.getText(1).getText());
        String expectedTpfText = "Lambrigger Paul\r\nIm Luss\r\nHaus Cebu\r\n3984 Fiesch";
        Assert.assertEquals(expectedTpfText, solo.getText(2).getText());
    }*/

  /*  public void testMultipleTimesSameBookmark() throws Exception {
        solo.clickOnText(SEARCH_TEXT);
        solo.enterText(0, "11771");
        solo.clickOnButton(1);
        solo.waitForActivity("ShowPlateOwnerActivity", 60000); // 1 minute timeout
        solo.clickOnText(ADD_BOOKMARK_TEXT);
        solo.goBack();
        solo.waitForActivity("WelcomeListActivity", 60000); // 1 minute timeout
        solo.clickOnText(SEARCH_TEXT);
        solo.enterText(0, "11771");
        solo.clickOnButton(1);
        solo.waitForActivity("ShowPlateOwnerActivity", 60000); // 1 minute timeout
        solo.clickOnText(REMOVE_BOOKMARK_TEXT);
        solo.clickOnText(ADD_BOOKMARK_TEXT);
        solo.waitForActivity("BookmarksViewActivity", 60000); // 1 minute timeout
        Assert.assertTrue(solo.searchText("FR 11 771", 1));
    }*/

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }
}
