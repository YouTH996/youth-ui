class FavApps {
    constructor() {
        if (window.carJsBridge.local("favAppData")) {
            this.favAppData = JSON.parse(window.carJsBridge.local("favAppData"));
        } else {
            if(localStorage.getItem("favAppData")){
                this.favAppData = JSON.parse(localStorage.getItem("favAppData"));
                window.carJsBridge.setLocal("favAppData",localStorage.getItem("favAppData"));
            }else{
                this.favAppData = {};
            }
        }
        this.themeData = {};
    }

    saveData(pkg, name) {
        if (!this.favAppData[pkg]) {
            this.favAppData[pkg] = name;
        }
        window.carJsBridge.setLocal("favAppData", JSON.stringify(this.favAppData));
    }

    saveNaviData(pkg, name) {
        const naviAppData = window.carJsBridge.local("naviAppData") ? JSON.parse(window.carJsBridge.local("naviAppData")) : {};
        if (!naviAppData[pkg]) {
            naviAppData[pkg] = name;
        }
        window.carJsBridge.setLocal("naviAppData", JSON.stringify(naviAppData));
    }

    updateFavList(){
        let apps = "";
        for (let pkg in this.favAppData){
            var pkglogo = pkg == "launcher3" ? "./img/thumb/ic_flat_launcher.png" : `${LOCAL_HOST}/appicon/${pkg}`;
            apps += `
                    <li pkg="${pkg}">
                        <img src="${pkglogo}">
						<p>${this.favAppData[pkg]}</p>
					</li>
                    `;
        }
        $(".app-fav-list ul").html(apps);
    }

    updateAllAppList(time){
        var appInfos = JSON.parse(window.carJsBridge.local("ALL_APP_LIST"));
        let apps = `
                    <li pkg="launcher3">
                        <img src="./img/thumb/ic_flat_launcher.png">
						<p>BYD桌面</p>
					</li>
                    `;
        for(var i = 0; i < appInfos.length; i++){
            apps += `
                    <li pkg="${appInfos[i].package_name}">
                        <img src="${this.getPkgLocalIcon(appInfos[i].package_name)}">
						<p>${appInfos[i].app_name}</p>
					</li>
                    `;
        }
        $(".app-list ul").html(apps);
    }

    updateUserAppList(cat){
        var that = this;
        var themeList = that.getThemeList();
        let apps = "";
        const appInfos = cat == "theme" ? themeList : JSON.parse(window.carJsBridge.local("ALL_APP_LIST"));
        for(var i = 0; i < appInfos.length; i++){
            if(appInfos[i].package_name.toLowerCase().indexOf(cat) !== -1 || cat == "theme" || (cat == "music" && /(kugou|com.maxmpz.audioplayer|com.afcabcbae|com.byd.mediacenter)/.test(appInfos[i].package_name))){
                var active = appInfos[i].package_name == window.carJsBridge.local("system_widget_app_"+ cat) ? "active" : "";
                var icon = cat == "theme" ? "" : `<img src="${LOCAL_HOST}/appicon/${appInfos[i].package_name}">`;
                apps += `
                    <li data-pkg="${appInfos[i].package_name}" class="${active}">
                        ${icon}
						<p>${appInfos[i].app_name}${cat != "theme" ? `(${appInfos[i].package_name})` : ""}</p>
					</li>
                    `;
            }
        }
        var ul = $(`.setting_select ul[data-cat="${cat}"]`);
        ul.html(apps);
        $(".setting_select").on("touchstart", "li",function(e){
            //console.log(e);
            e.preventDefault();	
            var pkg = $(this).data('pkg');
            var pkg_cat = $(this).parent().data('cat');
            var pkg_set = pkg_cat == "theme" ? that.themeData[pkg] : `<img src="${LOCAL_HOST}/appicon/${pkg}">${pkg}`;
            $(".default_app_"+ pkg_cat).html(pkg_set);
            $(this).addClass("active");
            //console.log(pkg);
            window.carJsBridge.setLocal("system_widget_app_"+ pkg_cat, pkg);
            if(pkg_cat == "theme") that.setCustomTheme(pkg);
        }).on("touchend", "li",function(e){
            ul.slideUp(400);
        });
        //$(".setting_select").show(500);
    }

    delData(pkg){
        delete this.favAppData[pkg];
        window.carJsBridge.setLocal("favAppData",JSON.stringify(this.favAppData));
    }

    getPkgLocalIcon(pkg){
        return "file:///sdcard/Android/data/com.liszt.blehelper/files/Pictures/AppIcons/"+pkg.replace(/\./g, "_")+".png";
    }

    setCustomTheme(themeId){
        var theme = "/theme/"+ themeId +"/theme.css";
        if(themeId != "default") theme = "http://liszt"+ theme;
        $("#skin").attr("href", theme);
        $(".widget_iframe").contents().find("#skin").attr("href", theme); 
        $(".navibar_iframe").contents().find("#skin").attr("href", theme); 
    }

    getThemeList(){
        this.themeData = JSON.parse(window.carJsBridge.local("THEME_DATA") || '{"default":"默认主题"}');
        var themeList = [];
        var keys = Object.keys(this.themeData);
        for (var i = 0; i < keys.length; i++) {
            var json = {};
            json["package_name"] = keys[i];
            json["app_name"] = this.themeData[keys[i]];
            themeList[i] = json;
        }
        return themeList;
    }

}