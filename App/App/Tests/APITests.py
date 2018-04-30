import unittest
import tempfile
import os
from App import api, app, db, user_datastore, security
from flask import json
from flask_security.utils import login_user


class APITestClass(unittest.TestCase):
    def setUp(self):
        app.config['TESTING'] = True
        app.config['LOGIN_DISABLED'] = True  
        app.config['DEBUG'] = False
        app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///temp.db'
        app.config['SECRET_KEY'] = 'super-secret'
        app.config['SECURITY_PASSWORD_SALT'] = 'super-secret'
        self.app = app.test_client()    
        db.drop_all()
        db.create_all()
        user_datastore.create_role(name='super', description='Can create admin users')
        user_datastore.create_role(name='admin', description='Can view reports log')
        user_datastore.create_role(name='user', description='Can create reports')
        user_datastore.create_user(email='test@test.net', password='password')
        user_datastore.create_user(email='admin@test.net', password='password')
        user_datastore.create_user(email='user@test.net', password='password')
        user_datastore.add_role_to_user('test@test.net', 'super')
        user_datastore.add_role_to_user('admin@test.net', 'admin')
        user_datastore.add_role_to_user('user@test.net', 'user')   
        db.session.commit()

    def tearDown(self):
        pass
           
    def test_report_routes(self):
        # Test empty query
        
        respone = self.app.post('/api/reports', content_type='application/json', data=json.dumps({}))
        self.assertEqual(respone.status_code,400)
       
        # Test valid query
        type = "Special"
        description = "Test report"
        address = "address"
        lat = "23728378273"
        lng = "37872387232"
        imagedata = "image data"
        respone = self.create_report(type, description, address, lat, lng, imagedata)
        self.assertNotEqual(respone.status_code,400)
        id = json.loads(respone.data)['reports']['id']

        # Check if report is in the database
        self.login('test@test.net','password')
        respone = self.app.get('/api/reports?page=1&perpage=200')
        self.assertEqual(respone.status_code,200)
        self.assertIn(id , [ r['id'] for r in json.loads(respone.data)['reports']])
        self.assertIn(description , [ r['description'] for r in json.loads(respone.data)['reports']])
        self.assertIn(int(lat) , [ r['lat'] for r in json.loads(respone.data)['reports']])

        # Test getting single report 
        respone = self.app.get('/api/reports/'+str(id))
        self.assertEqual(id , json.loads(respone.data)['reports']['id'])

        # Remove report from database
        respone = self.app.delete('/api/reports', content_type='application/json', data=json.dumps({'id':id}))
        self.assertEqual(respone.status_code,200)
        print(respone)
        
        # Check if report still exists
        respone = self.app.get('/api/reports')
        self.assertNotIn(id , [ r['id'] for r in json.loads(respone.data)['reports']])

        # Test deleting non existant report
        respone = self.app.delete('/api/reports', content_type='application/json', data=json.dumps({'id':id}))
        self.assertEqual(respone.status_code,404)
        
        # Test getting non existant single report
        respone = self.app.get('/api/reports/'+str(id))
        self.assertEqual(respone.status_code,404)

        # Test valid query with invalid report type
        type = "None"
        respone = self.create_report(type, description, address, lat, lng, imagedata)
        self.assertEqual(respone.status_code,400)
   
    def test_reports_limit(self):
        ids = []
        limit = 1000
        # create 1000 report
        for i in range(0, limit):
            type = "Special"
            description = "Test report"
            address = "address"
            lat = "-25.363"
            lng = "131.044"
            imagedata = "image data"
            respone = self.create_report(type, description, address, lat, lng, imagedata)
            self.assertNotEqual(respone.status_code,400)
            ids.append(json.loads(respone.data)['reports']['id'])

        # retreive 1000 reports one per page
        for i in range(0,limit):
            respone = self.app.get('/api/reports?page='+ str(i+1) +'&perpage=1')
            self.assertEqual(respone.status_code,200)
            self.assertIn(ids[i] , [ r['id'] for r in json.loads(respone.data)['reports']])
        
        # retreive 1000 reports 20 per page
        for i in range(0,limit//20):
            respone = self.app.get('/api/reports?page='+ str(i+1) +'&perpage=20')
            self.assertEqual(respone.status_code,200)
            for j in range(0,20):
                print("i:"+str(i))
                print("j:"+str(j))
                print(ids)
                print(json.loads(respone.data)['reports'])
                self.assertIn(ids[j+(i*20)] , [ r['id'] for r in json.loads(respone.data)['reports']])
        
        # delete 1000 reports
        for i in range(0,limit):
            respone = self.app.delete('/api/reports', content_type='application/json', data=json.dumps({'id':ids[i]}))
            self.assertEqual(respone.status_code,200)
            respone = self.app.get('/api/reports/'+str(ids[i]))
            self.assertEqual(respone.status_code,404)
    
    # TODO: test save image

    def test_distnace_functions(self):
        # test 1
        fromLat = 44.0625
        fromLng = -123.074117
        toLat = 44.072748
        toLng = -123.06936
        distance = 20
        dist = api.distance(fromLat, fromLng, toLat, toLng)
        bnd = api.bound(fromLat, fromLng, distance)
        dest = api.destination(fromLat, fromLng, 90, distance)    
        self.assertAlmostEqual(dist, 1.2012356922377)
        self.assertAlmostEqual(bnd['N']['lat'], 44.242364321184)
        self.assertAlmostEqual(bnd['S']['lat'], 43.882635678816)
        self.assertAlmostEqual(bnd['E']['lng'], -122.82381311894)
        self.assertAlmostEqual(bnd['W']['lng'], -123.32442088106)
        self.assertAlmostEqual(dest['lat'], 44.062226774476)
        self.assertAlmostEqual(dest['lng'], -122.82381311894)
        # test 2
        fromLat = 9.791751
        fromLng = 122.864107
        toLat = 12.383377
        toLng = 125.030976
        distance = 100
        dist = api.distance(fromLat, fromLng, toLat, toLng)
        bnd = api.bound(fromLat, fromLng, distance)
        dest = api.destination(fromLat, fromLng, 0, distance)    
        self.assertAlmostEqual(dist, 372.74889595764)
        self.assertAlmostEqual(bnd['N']['lat'], 10.691072605919)
        self.assertAlmostEqual(bnd['S']['lat'], 8.8924293940813)
        self.assertAlmostEqual(bnd['E']['lng'], 123.77672100788)
        self.assertAlmostEqual(bnd['W']['lng'], 121.95149299212)
        self.assertAlmostEqual(dest['lat'], 10.691072605919)
        self.assertAlmostEqual(dest['lng'], 122.864107)
        
    # Test report search
    def test_report_search(self):
        ids = []
        types = ["Routine","Special","Emergency"]
        # Test type search
        for type in types:
            description = "Test report search"
            address = "Bantayan - Bayawan Rd, Kabankalan, Negros Occidental, Philippines"
            lat = "9.702727"
            lng = "122.804295"
            imagedata = "image data"
            # add report
            respone = self.create_report(type, description, address, lat, lng, imagedata, 1)
            self.assertNotEqual(respone.status_code,400)
            id = json.loads(respone.data)['reports']['id']
            ids.append(id)
            # check if report was added
            respone = self.app.get('/api/reports/'+str(id))
            self.assertEqual(id , json.loads(respone.data)['reports']['id'])
            # search for report
            respone = self.app.get('/api/reports?perpage='+str(id+10)+'&type='+type)
            self.assertEqual(respone.status_code,200)
            self.assertIn(id , [ r['id'] for r in json.loads(respone.data)['reports']])
            self.assertIn(description , [ r['description'] for r in json.loads(respone.data)['reports']])
            # Test that other types dont appear in search result           
            if type == "Routine":
                self.assertNotIn("Special" , [ r['type'] for r in json.loads(respone.data)['reports']])
                self.assertNotIn("Emergency" , [ r['type'] for r in json.loads(respone.data)['reports']])
            elif type == "Special":
                self.assertNotIn("Routine" , [ r['type'] for r in json.loads(respone.data)['reports']])
                self.assertNotIn("Emergency" , [ r['type'] for r in json.loads(respone.data)['reports']])
            elif type == "Emergency":
                self.assertNotIn("Special" , [ r['type'] for r in json.loads(respone.data)['reports']])
                self.assertNotIn("Routine" , [ r['type'] for r in json.loads(respone.data)['reports']])
        
        # Test distance search
        # distance to test from
        lat1 = "11.174732"
        lng1 = "122.511834"
        distnace1 = "20"
        distnace2 = "100"
        # Report info
        description = "Test report search"
        address = "Bantayan - Bayawan Rd, Kabankalan, Negros Occidental, Philippines"
        imagedata = "image data"
        # create close report
        lat2 = "11.008530"
        lng2 = "122.505654"       
        respone = self.create_report(type, description, address, lat2, lng2, imagedata, 1)
        self.assertNotEqual(respone.status_code,400)
        id1 = json.loads(respone.data)['reports']['id']
        ids.append(id1)
        # create far report
        lat3 = "10.762730"
        lng3 = "123.241052"
        respone = self.create_report(type, description, address, lat3, lng3, imagedata)
        self.assertNotEqual(respone.status_code,400)
        id2 = json.loads(respone.data)['reports']['id']
        ids.append(id2)
        # search for reports within 20 km
        respone = self.app.get('/api/reports?perpage='+str(id+10)+'&distance='+distnace1+'&lat='+lat1+'&lng='+lng1)
        self.assertEqual(respone.status_code,200)
        self.assertIn(id1 , [ r['id'] for r in json.loads(respone.data)['reports']])
        self.assertNotIn(id2 , [ r['id'] for r in json.loads(respone.data)['reports']])
        # search for reports within 100 km
        respone = self.app.get('/api/reports?perpage='+str(id+10)+'&distance='+distnace2+'&lat='+lat1+'&lng='+lng1)
        self.assertEqual(respone.status_code,200)
        self.assertIn(id1 , [ r['id'] for r in json.loads(respone.data)['reports']])
        self.assertIn(id2 , [ r['id'] for r in json.loads(respone.data)['reports']])

        # TOD0: Test distnace and type search

        # TODO: Test user search
        # TODO: delete reports

    # Helper functions
    def create_report(self, type, description, address, lat, lng, imagedata, userid):
        return self.app.post('/api/reports', data=json.dumps({'type':type, 'description':description, 'user_id': userid,
                                                             'address':address, 'lat':lat, 'lng':lng, 'imagedata':imagedata}),
                            content_type='application/json')
    def login(self, username, password):
        return self.app.post('/login', data=dict(username=username,password=password), follow_redirects=True)

    def logout(self):
        return self.app.get('/logout', follow_redirects=True)

if __name__ == '__main__':
    unittest.main()