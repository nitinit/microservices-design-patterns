import React, { Component } from 'react';
import '../App.css';
import AppNavbar from './AppNavbar';
import { get } from '../services/ApiService';
import HomeContent from './HomeContent';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
class Home extends Component {
  constructor(props) {
    super(props);
    this.logout = this.logout.bind(this);
  }

  async logout() {
    try {
      await get('/logout')
      this.props.removeAuthentication();
    } catch (error) {
      console.error(error);
    }
  }
  displayMessage = () => {
    const { error } = this.props;
    if(error) {
      return <MessageAlert {... errorMessage(error)}></MessageAlert>
    }
    return ''
  }

  render() {
    return (
      <div>
        <AppNavbar />
        {this.displayMessage()}
        <HomeContent {...this.props}></HomeContent>
      </div>
    );
  }
}




export default Home;