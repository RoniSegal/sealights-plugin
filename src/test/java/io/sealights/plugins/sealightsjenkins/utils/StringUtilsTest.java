package io.sealights.plugins.sealightsjenkins.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ronis on 4/26/2017.
 */
public class StringUtilsTest {

    @Test
    public void trimStart_lengthGraterThen10_shouldReturnTrimmedString(){

        //Arrange
        String original = "abcdefghijk";

        //Act
        String trimmed = StringUtils.trimStart(original);

        //Assert
        Assert.assertEquals("..."+original.substring(original.length()-10),trimmed);
    }

    @Test
    public void trimStart_lengthLowerThen10_shouldReturnOriginalString(){

        //Arrange
        String original = "abcdefg";

        //Act
        String trimmed = StringUtils.trimStart(original);

        //Assert
        Assert.assertEquals(original,trimmed);
    }

    @Test
    public void trimStart_strIsNull_shouldReturnNull(){

        //Arrange
        String original =null;

        //Act
        String trimmed = StringUtils.trimStart(original);

        //Assert
        Assert.assertEquals(original,trimmed);

    }


    @Test
    public void trimStartByValue_lengthGraterThen10_shouldReturnTrimmedString(){

        //Arrange
        String original = "abcdefghijk";

        //Act
        String trimmed = StringUtils.trimStart(original,5);

        //Assert
        Assert.assertEquals("..."+original.substring(original.length()-5),trimmed);
    }

    @Test
    public void trimStartByValue_lengthLowerThenWantedLength_shouldReturnOriginalString(){

        //Arrange
        String original = "abcdefghijk";

        //Act
        String trimmed = StringUtils.trimStart(original,17);

        //Assert
        Assert.assertEquals(original,trimmed);
    }

    @Test
    public void convertKeyValueStringToMap_multipleParams_shouldReturnMap(){
        //Arrange
        String original = "param1:val1$$param2:val2";
        Map<String, String> expected = new HashMap<>();
        expected.put("param1", "val1");
        expected.put("param2", "val2");

        //Act
        Map<String, String> actual = StringUtils.convertKeyValueStringToMap(original);

        //Assert
        Assert.assertEquals(actual, expected);


    }

    @Test
    public void convertKeyValueStringToMap_colonInValue_shouldSplitByFirst(){
        //Arrange
        String original = "url:http://some.com";
        Map<String, String> expected = new HashMap<>();
        expected.put("url", "http://some.com");

        //Act
        Map<String, String> actual = StringUtils.convertKeyValueStringToMap(original);

        //Assert
        Assert.assertEquals(actual, expected);


    }

    @Test
    public void convertKeyValueStringToMap_multipleColonInValue_shouldSplitByFirst(){
        //Arrange
        String original = "url:http://some.com:8080";
        Map<String, String> expected = new HashMap<>();
        expected.put("url", "http://some.com:8080");

        //Act
        Map<String, String> actual = StringUtils.convertKeyValueStringToMap(original);

        //Assert
        Assert.assertEquals(actual, expected);


    }

    @Test
    public void convertKeyValueStringToMap_nullString_shouldReturnEmptyMap(){
        //Arrange
        String original = null;

        //Act
        Map<String, String> actual = StringUtils.convertKeyValueStringToMap(original);

        //Assert
        Assert.assertEquals(actual.size(), 0);


    }


}
