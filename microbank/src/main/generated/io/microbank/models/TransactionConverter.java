/*
 * Copyright (c) 2014 Red Hat, Inc. and others
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.microbank.models;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.microbank.models.Transaction}.
 *
 * NOTE: This class has been automatically generated from the {@link io.microbank.models.Transaction} original class using Vert.x codegen.
 */
public class TransactionConverter {

  public static void fromJson(JsonObject json, Transaction obj) {
    if (json.getValue("txAmount") instanceof Number) {
      obj.setTxAmount(((Number)json.getValue("txAmount")).doubleValue());
    }
    if (json.getValue("txFrom") instanceof String) {
      obj.setTxFrom((String)json.getValue("txFrom"));
    }
    if (json.getValue("txStatus") instanceof Boolean) {
      obj.setTxStatus((Boolean)json.getValue("txStatus"));
    }
    if (json.getValue("txTo") instanceof String) {
      obj.setTxTo((String)json.getValue("txTo"));
    }
  }

  public static void toJson(Transaction obj, JsonObject json) {
    json.put("txAmount", obj.getTxAmount());
    if (obj.getTxFrom() != null) {
      json.put("txFrom", obj.getTxFrom());
    }
    json.put("txStatus", obj.isTxStatus());
    if (obj.getTxTo() != null) {
      json.put("txTo", obj.getTxTo());
    }
  }
}