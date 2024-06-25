package com.hmsnetworks.sc.cumulocity.data;

import com.hms_networks.americas.sc.extensions.string.StringUtils;
import java.util.List;

/**
 * Class to parse and represent tag names in the formats supported by the connector.
 *
 * @since 1.4.0
 * @version 1.0.0
 * @author HMS Networks, MU Americas Solution Center
 */
public class CConnectorTagName {

  /**
   * The index of the child device name in the split tag name array returned by {@link
   * #getSplitTagName(String)}.
   *
   * @since 1.0.0
   */
  private static final int SPLIT_TAG_NAME_INDEX_CHILD_DEVICE = 0;

  /**
   * The index of the fragment name in the split tag name array returned by {@link
   * #getSplitTagName(String)}.
   *
   * @since 1.0.0
   */
  private static final int SPLIT_TAG_NAME_INDEX_FRAGMENT = 1;

  /**
   * The index of the series name in the split tag name array returned by {@link
   * #getSplitTagName(String)}.
   *
   * @since 1.0.0
   */
  private static final int SPLIT_TAG_NAME_INDEX_SERIES = 2;

  /**
   * The size of the array with expected tag name components.
   *
   * @since 1.0.0
   */
  private static final int SPLIT_TAG_NAME_ARRAY_SIZE = 3;

  /**
   * The delimiter used to split the tag name into components.
   *
   * @since 1.0.0
   */
  public static final String SPLIT_TAG_NAME_DELIMITER = "/";

  /**
   * The default Cumulocity data series name if one is not included in the tag name.
   *
   * @since 1.0.0
   */
  private static final String DEFAULT_SERIES_VALUE = "0";

  /**
   * The name of the child device.
   *
   * @since 1.0.0
   */
  private final String childDevice;

  /**
   * The name of the fragment.
   *
   * @since 1.0.0
   */
  private final String fragment;

  /**
   * The name of the series.
   *
   * @since 1.0.0
   */
  private final String series;

  public CConnectorTagName(String childDevice, String fragment, String series) {
    this.childDevice = childDevice;
    this.fragment = fragment;
    this.series = series;
  }

  public CConnectorTagName(String tagName) {
    String[] datapointSplitTagName = getSplitTagName(tagName);
    this.childDevice = datapointSplitTagName[SPLIT_TAG_NAME_INDEX_CHILD_DEVICE];
    this.fragment = datapointSplitTagName[SPLIT_TAG_NAME_INDEX_FRAGMENT];
    this.series = datapointSplitTagName[SPLIT_TAG_NAME_INDEX_SERIES];
  }

  /**
   * Returns the name of the child device. If the tag name does not include a child device name,
   * this method will return {@code null}.
   *
   * @return child device name
   * @since 1.0.0
   */
  public String getChildDevice() {
    return childDevice;
  }

  /**
   * Returns the name of the fragment.
   *
   * @return fragment name
   * @since 1.0.0
   */
  public String getFragment() {
    return fragment;
  }

  /**
   * Returns the name of the fragment surrounded by quotes. This is especially useful for building
   * payloads which follow the Cumulocity SmartREST format.
   *
   * @return fragment name
   * @since 1.0.0
   */
  public String getFragmentQuoted() {
    return "\"" + fragment + "\"";
  }

  /**
   * Returns the name of the series. If the tag name does not include a series name, this method
   * will return {@code DEFAULT_SERIES_VALUE}.
   *
   * @return series name
   * @since 1.0.0
   */
  public String getSeries() {
    return series;
  }

  /**
   * Splits a given tag name in to its expected components (child device, fragment, series).
   *
   * @param tagName tag name to split into expected components
   * @return tag name components (child device, fragment, series)
   * @since 1.0.0
   */
  public static String[] getSplitTagName(String tagName) {
    // Split the tag name into its component parts
    List tagNameComponents = StringUtils.split(tagName, SPLIT_TAG_NAME_DELIMITER);

    // Build the split tag name array
    final int sizeOnlyFragment = 1;
    final int sizeFragmentAndSeries = 2;
    final int sizeFragmentSeriesAndChildDevice = 3;
    final int indexChildDevice = 0;
    final int indexFragment = 1;
    final int indexSeries = 2;
    int firstComponentIndex = 0;
    int secondComponentIndex = 1;
    int thirdComponentIndex = 2;
    String[] splitTagName = new String[SPLIT_TAG_NAME_ARRAY_SIZE];
    switch (tagNameComponents.size()) {
      case sizeOnlyFragment:
        splitTagName[indexChildDevice] = null;
        splitTagName[indexFragment] = (String) tagNameComponents.get(firstComponentIndex);
        splitTagName[indexSeries] = DEFAULT_SERIES_VALUE;
        break;
      case sizeFragmentAndSeries:
        splitTagName[indexChildDevice] = null;
        splitTagName[indexFragment] = (String) tagNameComponents.get(firstComponentIndex);
        splitTagName[indexSeries] = (String) tagNameComponents.get(secondComponentIndex);
        break;
      case sizeFragmentSeriesAndChildDevice:
        splitTagName[indexChildDevice] = (String) tagNameComponents.get(firstComponentIndex);
        splitTagName[indexFragment] = (String) tagNameComponents.get(secondComponentIndex);
        splitTagName[indexSeries] = (String) tagNameComponents.get(thirdComponentIndex);
        break;
      default:
        // Get last three components of the tag name (truncate additional leading components)
        firstComponentIndex = tagNameComponents.size() - 3;
        secondComponentIndex = tagNameComponents.size() - 2;
        thirdComponentIndex = tagNameComponents.size() - 1;
        splitTagName[indexChildDevice] = (String) tagNameComponents.get(firstComponentIndex);
        splitTagName[indexFragment] = (String) tagNameComponents.get(secondComponentIndex);
        splitTagName[indexSeries] = (String) tagNameComponents.get(thirdComponentIndex);
        break;
    }
    return splitTagName;
  }
}
