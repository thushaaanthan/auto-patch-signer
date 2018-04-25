 import React from 'react';
 import request from '../node_modules/superagent/superagent';
 import Select from 'react-select';
 import Request from 'react-http-request';
 import Validator from 'react-validation';

 const configData = require('./resouses/config');
 const URL_FETCH_DATA = configData.SERVER_URL_FETCH_DATA;
 const URL_SHOW_RESULT = configData.SERVER_URL_SHOW_RESULT;
 const URL_VALIDATE_PATCH = configData.SERVER_URL_VALIDATE_PATCH;

class App extends React.Component {

    constructor(){
        super();
        // this.handleSubmit = this.handleSubmit.bind(this);
        this.state = {carbonV: "", patchId: "",status:"",productName:"",developer:"",processState:false,validVersion:false,validId:false,validStatus:false,validProductName:false,
                            validDeveloper:false, errorMsg:"",firstInput:true,isValid:false,requestShow:""}
    }



    changeHeader = (e) => {
        e.preventDefault();
        if(this.state.firstInput){
            this.setState({firstInput: false});
        }
        if(this.state.validProductName && this.state.validStatus && this.state.validVersion && (this.state.developer.length > 0) && (this.state.patchId.length === 4)) {
            this.setState({processState: true});
            request
                .post(URL_VALIDATE_PATCH)
                .set('Content-Type', 'application/x-www-form-urlencoded')
                .send("version=" + this.state.carbonV.value + "&patchId=" + this.state.patchId + "&state=" + this.state.status.value + "&product=" + this.state.productName.value + "&developedBy=" + this.state.developer)
                .end((function (err, res) {
                    console.log('done');
                    console.log(res.text);
                    this.setState({requestShow:res.text})
                }).bind(this));

        }

    };



    changePage = (e) => {
        e.preventDefault();
        window.location.reload();
    };

    versionChange = (value) => {
        this.setState({validVersion:true});
        this.setState({ carbonV:value });
    };
    statusChange = (value) => {
        this.setState({validStatus:true});
        this.setState({ status:value });
    };
    productChange = (value) => {
        this.setState({validProductName:true});
        this.setState({ productName:value });
    };

    onChangePatchId(event) {
        this.setState({patchId: event.target.value});
    }
    onChangeDeveloper(event) {
        this.setState({developer: event.target.value});
    };

    render () {
        const appStyle = {
            fontFamily: "Helvetica",
            fontSize: "20",
            align: "left",
            marginLeft: 50
        };
        const selectStyle = {
            maxWidth: 365,
            fontSize: 16
        };
        const h1Style = {
            fontFamily: "Helvetica",
            fontSize: 20,
            fontWeight: 700,
        };
        const titleStyle = {
            fontFamily: "Helvetica",
            fontSize: 18,
            fontWeight: 400,
            color: "#484848"
        };
        const errorStyle = {
            fontFamily: "Helvetica",
            fontSize: 18,
            fontWeight: 400,
            color: "#aa1212"
        };
        const inputStyle = {
            fontFamily: "Helvetica",
            fontSize: 16,
            width: 350,
            height: 30,
        };
        const buttonStyle = {
            fontFamily: "Helvetica",
            fontSize: "22",
            backgroundColor: "#1BADF8",
            paddingTop: 10,
            paddingBottom: 10,
            paddingLeft: 20,
            paddingRight: 20,
            color: "#fff",
            fontWeight: 800,
            borderRadius: "10%",
            boxShadow: 0,
        };
        const resetButtonStyle = {
            fontFamily: "Helvetica",
            fontSize: "22",
            backgroundColor: "#882121",
            paddingTop: 10,
            paddingBottom: 10,
            paddingLeft: 20,
            paddingRight: 20,
            color: "#fff",
            fontWeight: 800,
            borderRadius: "10%",
            boxShadow: 0,
        };

        const processState = this.state.processState;
        const processDisplay = processState ? (
            <p>Process started... </p>
        ) : (
            <p>Please fill and click signing button</p>
        );

        let reg = /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/ ;
        const validEmail = (reg.test(this.state.developer));
        const validId = this.state.patchId.length === 4;
        const isEnabled = (this.state.validProductName && this.state.validStatus && this.state.validVersion && validEmail && validId) || (this.state.firstInput);
        const errMsg = isEnabled ? (<p></p>):(<p style={errorStyle}>Whoops! There are some problems with your inputs </p>);


        return (


            <div id="signup" style={appStyle}>


                <h1 style={h1Style}>Auto Sign (PMT parameter generator)</h1>

                <p style={titleStyle}>{ this.state.validId }Carbon Version</p>
                <div style={selectStyle}>
                    <Select
                        name="Carbon Version"
                        onChange={this.versionChange.bind(this)}
                        options={[
                            { "value" : 'wilkes', "label": 'Wilkes' },
                            { value: 'perlis', label: 'Perlis' },
                            { value: 'turing', label: 'Turing' },
                        ]}
                    />
                </div>

                <p style={titleStyle}>Patch Id ( 4-digit number )</p>
                <input type="text" placeholder="enter patch id" style={inputStyle} onChange={this.onChangePatchId.bind(this)}/>

                <p style={titleStyle}>State</p>
                <div style={selectStyle}>
                    <Select
                        name="Patch Id"
                        onChange={this.statusChange.bind(this)}
                        options={[
                            { value: '1', label: 'Released not in public SVN' },
                            { value: '2', label: 'Released not automated' },
                            { value: '3', label: 'Released' },
                        ]}
                    />
                </div>

                <p style={titleStyle}>Product</p>
                <div style={selectStyle}>

                    <Request
                        url={URL_FETCH_DATA}
                        method='get'
                        accept='json'
                    >
                        {
                            ({error, result, loading}) => {
                                if (loading) {
                                    return <div align="center"><p align="center">loading...</p></div>;
                                } else {
                                    return(<Select
                                        name="Product"
                                        onChange={this.productChange.bind(this)}
                                        options={result.body}
                                    />)

                                }}}
                    </Request>

                </div>

                <p style={titleStyle}>Developer</p>
                <input type="email" placeholder="enter developer's email" value={this.state.developer} style={inputStyle} onChange={this.onChangeDeveloper.bind(this)}/>

                <div>
                    <div>{errMsg}</div>
                    <form onSubmit={this.changeHeader} >
                        <br/><button disabled={!isEnabled} type="Submit" style={buttonStyle}>VALIDATE AND SIGN</button>
                    </form>
                    <form onSubmit={this.changePage} >
                        <br/><button type="Submit" style={resetButtonStyle}>RESET</button>
                    </form>
                </div>
                <div>{processDisplay}</div>
                <div>
                    <p>{this.state.requestShow}</p>
                    {/*<Request*/}
                        {/*url={URL_SHOW_RESULT}*/}
                        {/*method='get'*/}
                        {/*accept='json'*/}
                    {/*>*/}
                        {/*{*/}
                            {/*({error, result, loading}) => {*/}
                                {/*if (loading) {*/}
                                    {/*return <div align="left"><p align="left"><br/></p></div>;*/}
                                {/*} else {*/}
                                    {/*return(<p align="left">*/}
                                        {/*{result.body[0]["result"]}*/}
                                        {/*</p>*/}
                                    {/*)*/}

                                {/*}}}*/}
                    {/*</Request>*/}
                </div>​
            </div>

        )
    }
}

export default App;
