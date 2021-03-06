/*
 * Copyright 2014 Corpuslinguistic working group Humboldt University Berlin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package annis.gui.converter;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.vaadin.data.util.converter.Converter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
public class CommaSeperatedStringConverterList implements Converter<String, List>
{

  private static final Splitter splitter = Splitter.on(',').trimResults().
    omitEmptyStrings();

  private static final Joiner joiner = Joiner.on(", ");

  @Override
  public List convertToModel(String value,
    Class<? extends List> targetType, Locale locale) throws ConversionException
  {
    List<String> result = new ArrayList<>();
    for(String s : splitter.split(value))
    {
      result.add(s);
    }
    return result;
  }

  @Override
  public String convertToPresentation(List value,
    Class<? extends String> targetType, Locale locale) throws ConversionException
  {
    return joiner.join(value);
  }

  @Override
  public Class<String> getPresentationType()
  {
    return String.class;
  }

  @Override
  public Class<List> getModelType()
  {
    return List.class;
  }
  
}
