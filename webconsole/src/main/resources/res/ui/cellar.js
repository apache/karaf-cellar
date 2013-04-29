/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function renderGroups(data) {
    $(document).ready(function() {
        renderView();
        renderData(data);
    });
}

function renderView() {
    renderStatusLine();
    renderTable("Cluster Nodes", "group_table", ["Group", "Node", "Actions"]);

    var txt = "<form method='post'><table id='group_table_footer' class='ui-widget-header nicetable noauto ui-widget'><tbody>" +
            "<tr><input type='hidden' name='action' value='createGroup'/>" +
            "<td><input id='groupName' type='text' name='groupName' style='width:100%' colspan='2'/></td>" +
            "<td class='col_Actions'><input type='button' value='Create Group' onclick='addGroup()'/></td>" +
            "</tr></tbody></table></form><br/>";

    $("#plugin_content").append(txt);

}

function addGroup() {
    var groupName = document.getElementById("groupName").value;
    changeGroupListState("createGroup", groupName);
}

function changeGroupListState(/* String */ action, /* String */ groupName) {
    $.post(pluginRoot, {"action": action, "group": groupName}, function(data) {
        renderData(data);
    }, "json");
}

function renderStatusLine() {
    $("#plugin_content").append("<div class='fullwidth'><div class='statusline'/></div>");
}

function renderTable(/* String */ title, /* String */ id, /* array of Strings */ columns) {
    var txt = "<div class='ui-widget-header ui-corner-top buttonGroup'><table class='nicetable ui-widget'><thead><tr>" +
            "<th>" +
            title + "</th></tr></thead></table></div>" +
            "<table id='" + id + "' class='nicetable ui-widget'><thead><tr>";
    for (var name in columns) {
        txt = txt + "<th class='col_" + columns[name] + " ui-widget-header header' >" + columns[name] + "</th>";
    }
    txt = txt + "</tr></thead><tbody></tbody></table>";
    $("#plugin_content").append(txt);
}

function renderData(/* Object */ data) {
    renderStatusData(data.status);
    renderGroupTableData(data);
    $("#group_table").tablesorter({
        headers: {
            2: { sorter: false }
        },
        sortList: [
            [0,0]
        ],
    });
}

function renderStatusData(/* String */ status) {
    $(".statusline").empty().append(status);
}

function renderGroupTableData(/* array of Objects */ data) {
    var trElement;
    var input;
    var needsLegend = false;
    $("#group_table > tbody > tr").remove();
    for (var idx in data.groups) {
        var name = data.groups[idx].name;
        trElement = tr(null, { id: "group-" + name });
        renderGroupMemberData(trElement, data.groups[idx]);
        $("#group_table > tbody").append(trElement);
        if (data.groups[ name.length - 1 ] == "*") {
            needsLegend = true;
        }
    }
    $("#group_table").trigger("update");
    if (needsLegend) {
        trElement = tr(null, null);
        trElement.appendChild(td(null, { colspan: 3 },
                [ text("* Installed via deploy directory") ]));
        $("#group_table_footer > tbody").prepend(trElement);
    }
    $("#group_table_footer").trigger("update");
}

/**
 * Add under the parent element a group/member/actions row
 * @param parent
 * @param group
 */
function renderGroupMemberData(/* Element */ parent, /* Object */ group) {
    if (group.members.length > 0) {
        for (var idx in group.members) {
            parent.appendChild(td(null, null, [ text(group.name) ]));
            parent.appendChild(td(null, null, [ text(group.members[idx].id) ]));

            var actionsTd = td(null, null);
            var div = createElement("div", null, {
                style: { "text-align": "left"}
            });
            actionsTd.appendChild(div);

            for (var a in group.actions) {
                groupButton(div, group.name, "", group.actions[a]);
            }
            parent.appendChild(actionsTd);
        }
    } else {
        parent.appendChild(td(null, null, [ text(group.name) ]));
        parent.appendChild(td(null, null, [ text("") ]));

        var actionsTd = td(null, null);
        var div = createElement("div", null, {
            style: { "text-align": "left"}
        });
        actionsTd.appendChild(div);

        for (var a in group.actions) {
            groupButton(div, group.name, "", group.actions[a]);
        }
        parent.appendChild(actionsTd);
    }
}

function groupButton(/* Element */ parent, /* String */ group, /* String */ node, /* Obj */ action) {
    if (!action.enabled) {
        return;
    }

    var input = createElement("input", null, {
        type: 'image',
        style: {"margin-left": "10px"},
        title: action.title,
        alt: action.title,
        src: imgRoot + '/bundle_' + action.image + '.png'
    });
    $(input).click(function() {
        changeGroupState(action.op, group, node)
    });

    if (!action.enabled) {
        $(input).attr("disabled", true);
    }
    parent.appendChild(input);
}

function changeGroupState(/* String */ action, /* String */ group, /* String */ node) {
    $.post(pluginRoot, {"action": action, "group": group, "node": node}, function(data) {
        renderData(data);
    }, "json");
}

function renderFeatureTableData(/* array of Objects */ features) {
    $("#feature_table > tbody > tr").remove();
    for (var idx in features) {
        var trElement = tr(null, { id: "feature-" + features[idx].id });
        renderFeatureData(trElement, features[idx]);
        $("#feature_table > tbody").append(trElement);
    }
    $("#feature_table").trigger("update");
}
