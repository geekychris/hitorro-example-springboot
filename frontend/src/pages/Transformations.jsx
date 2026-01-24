import React from 'react'
import { useQuery } from 'react-query'
import {
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
  CircularProgress,
} from '@mui/material'
import TransformIcon from '@mui/icons-material/Transform'
import { transformerApi } from '../services/api'

function Transformations() {
  const { data, isLoading, error } = useQuery(
    'transformations',
    () => transformerApi.getAvailableTransformations()
  )

  if (isLoading) {
    return (
      <Box className="loading-spinner">
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box>
        <Typography color="error">
          Error loading transformations: {error.message}
        </Typography>
      </Box>
    )
  }

  const transformations = data?.data || []

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Content Transformations
      </Typography>
      <Typography variant="body1" color="textSecondary" paragraph>
        Convert documents between different formats
      </Typography>

      {transformations.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" color="textSecondary">
            No transformations available
          </Typography>
          <Typography variant="body2" color="textSecondary" sx={{ mt: 1 }}>
            Check your transformer service configuration
          </Typography>
        </Paper>
      ) : (
        <Grid container spacing={3}>
          {transformations.map((transformation, index) => (
            <Grid item xs={12} sm={6} md={4} key={index}>
              <Card>
                <CardContent>
                  <Box display="flex" alignItems="center" mb={2}>
                    <TransformIcon color="primary" sx={{ mr: 1 }} />
                    <Typography variant="h6">
                      {transformation.name || `Transformation ${index + 1}`}
                    </Typography>
                  </Box>
                  
                  <Typography variant="body2" color="textSecondary" paragraph>
                    {transformation.description || 'No description available'}
                  </Typography>

                  <Box sx={{ mb: 1 }}>
                    <Typography variant="caption" color="textSecondary">
                      From:
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 0.5 }}>
                      {transformation.sourceFormats?.map((format) => (
                        <Chip key={format} label={format} size="small" />
                      )) || <Chip label="Any" size="small" />}
                    </Box>
                  </Box>

                  <Box>
                    <Typography variant="caption" color="textSecondary">
                      To:
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 0.5 }}>
                      {transformation.targetFormats?.map((format) => (
                        <Chip key={format} label={format} size="small" color="primary" />
                      )) || <Chip label="Various" size="small" color="primary" />}
                    </Box>
                  </Box>
                </CardContent>
                
                <CardActions>
                  <Button size="small" color="primary">
                    Use Transformation
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}
    </Box>
  )
}

export default Transformations
